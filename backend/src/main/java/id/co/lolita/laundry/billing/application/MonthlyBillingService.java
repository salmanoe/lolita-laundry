package id.co.lolita.laundry.billing.application;

import id.co.lolita.laundry.billing.domain.BillingCycle;
import id.co.lolita.laundry.billing.domain.BillingStatus;
import id.co.lolita.laundry.billing.domain.MonthlyBilling;
import id.co.lolita.laundry.billing.domain.MonthlyBillingLine;
import id.co.lolita.laundry.billing.domain.port.in.GenerateMonthlyBillingUseCase;
import id.co.lolita.laundry.billing.domain.port.in.SyncOrderBillingUseCase;
import id.co.lolita.laundry.billing.domain.port.in.UpdateBillingStatusUseCase;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway.ClientInfo;
import id.co.lolita.laundry.billing.domain.port.out.BillingStoragePort;
import id.co.lolita.laundry.billing.domain.port.out.CompanyProfileGateway;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway.DeliveredOrder;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.CompanyHeader;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.MonthlyBillingDocument;
import id.co.lolita.laundry.billing.domain.port.out.MonthlyBillingRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Generates Monthly Billings and drives their {@code DRAFT → ISSUED → PAID} lifecycle.
 *
 * <p>A COMBINED client yields one billing per month; a PER_DEPARTMENT client (PBS) yields one
 * per department that has delivered orders. Regeneration replaces an existing DRAFT but is
 * rejected once a billing is ISSUED or PAID.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
class MonthlyBillingService implements GenerateMonthlyBillingUseCase, UpdateBillingStatusUseCase,
        SyncOrderBillingUseCase {

    private static final String BILLING_NUMBER_PREFIX = "BILL-";

    private final MonthlyBillingRepository billingRepository;
    private final DeliveredOrderGateway deliveredOrders;
    private final BillingClientGateway clients;
    private final CompanyProfileGateway companyProfile;
    private final InvoicePdfPort pdf;
    private final BillingStoragePort storage;
    // The single-thread executor that serializes the async order→billing sync. Manual rebuilds run
    // on it too so a rebuild and an auto-sync for the same client/period can never overlap (KI-4).
    private final Executor billingEventExecutor;
    // Self-reference so the rebuild runs through the @Transactional proxy on the executor thread
    // (a direct this.* call would bypass it). Lazy → no init cycle.
    private final ObjectProvider<MonthlyBillingService> self;

    /**
     * Manual rebuild of a period's DRAFT. Dispatched onto the single-thread {@code
     * billingEventExecutor} and awaited, so it is serialized with the async order→billing sync
     * (KI-4): a sync event firing mid-rebuild can no longer resurrect a removed line or collide.
     * Non-transactional itself — the actual work opens its transaction on the executor thread.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<MonthlyBilling> generate(GenerateCommand command) {
        return runOnBillingThread(() -> self.getObject().generateInternal(command));
    }

    @Transactional
    public List<MonthlyBilling> generateInternal(GenerateCommand command) {
        var client = clients.findById(command.clientId())
                .orElseThrow(() -> new NotFoundException("Client not found: " + command.clientId()));

        // The period's calendar range, resolved from the client's billing cycle — a plain calendar
        // month unless the client bills on a cut-off (e.g. 26th → 25th).
        var cycle = client.cycle();
        var period = YearMonth.of(command.year(), command.month());
        var periodStart = cycle.startOf(period);
        var periodEnd = cycle.endOf(period);

        // KI-8 guard: a period's DRAFT may hold orders rolled forward from a frozen natural period
        // (their order_date falls outside this period's range). A manual rebuild keys membership
        // purely by order_date, so it cannot reproduce those rolled-in orders — replacing the DRAFT
        // would silently drop them (lost revenue). Such a period is fully auto-maintained; refuse
        // rather than corrupt it.
        boolean hasRolledForward = billingRepository.findAll(command.clientId(), command.year(), command.month())
                .stream()
                .filter(b -> b.getStatus() == BillingStatus.DRAFT)
                .flatMap(b -> b.getLines().stream())
                .anyMatch(l -> l.orderDate().isBefore(periodStart) || l.orderDate().isAfter(periodEnd));
        if (hasRolledForward) {
            throw new IllegalArgumentException(
                    ("Tagihan %s untuk %s memuat order yang digulirkan dari periode lain dan dikelola otomatis — "
                            + "regenerasi manual tidak tersedia.")
                            .formatted(BillingFormats.periodLabel(command.year(), command.month()), client.name()));
        }

        var billable = deliveredOrders.findBillableOrders(command.clientId(), periodStart, periodEnd);
        if (billable.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada order untuk %s pada periode %s"
                    .formatted(client.name(), BillingFormats.periodLabel(command.year(), command.month())));
        }

        List<MonthlyBilling> results = new ArrayList<>();
        if (client.perDepartment()) {
            // One billing per department; each order contributes its per-department portion.
            Map<Long, List<MonthlyBillingLine>> linesByDept = new LinkedHashMap<>();
            Map<Long, String> deptNames = new LinkedHashMap<>();
            for (var order : billable) {
                for (var portion : portionsOf(order, true)) {
                    linesByDept.computeIfAbsent(portion.departmentId(), _ -> new ArrayList<>())
                            .add(MonthlyBillingLine.of(order.orderId(), order.orderNumber(),
                                    order.orderDate(), portion.subtotal()));
                    deptNames.putIfAbsent(portion.departmentId(), portion.departmentName());
                }
            }
            for (var entry : linesByDept.entrySet()) {
                results.add(buildAndSave(client, entry.getKey(), deptNames.get(entry.getKey()),
                        period, periodStart, periodEnd, entry.getValue()));
            }
        } else {
            var lines = billable.stream()
                    .map(o -> MonthlyBillingLine.of(o.orderId(), o.orderNumber(), o.orderDate(), o.total()))
                    .toList();
            results.add(buildAndSave(client, null, null, period, periodStart, periodEnd, lines));
        }
        return results;
    }

    @Override
    public MonthlyBilling updateStatus(UpdateStatusCommand command) {
        var billing = billingRepository.findById(command.billingId())
                .orElseThrow(() -> new NotFoundException("Billing not found: " + command.billingId()));
        boolean issuing = billing.getStatus() == BillingStatus.DRAFT && command.target() == BillingStatus.ISSUED;
        billing.advanceStatus(command.target());
        if (issuing) {
            // Freeze the company letterhead + the client's bank account onto the billing at issue
            // time, then re-render so the issued PDF is self-contained and immune to later profile
            // edits or a reassignment of the client to a different account.
            var client = clients.findById(billing.getClientId())
                    .orElseThrow(() -> new NotFoundException("Client not found: " + billing.getClientId()));
            var c = companyProfile.current();
            var bank = companyProfile.bankAccount(client.bankAccountId());
            billing.captureCompany(c.companyName(), c.address(), c.phone(), bank.beneficiary(),
                    bank.bankName(), bank.accountNumber(), bank.accountHolder());
            // A DRAFT is stamped with the day its first order arrived. For a client on a cut-off
            // cycle that date lands ~a month before the invoice actually goes out and is
            // contractually meaningful, so re-stamp it with the real issue date. Calendar clients
            // keep the existing behaviour.
            if (client.billingCycleDay() != null) {
                billing.stampInvoiceDate(LocalDate.now());
            }
            var pdfBytes = pdf.renderMonthlyBilling(toDocument(billing, client));
            billing.attachPdf(storage.store("billings/" + billing.getBillingNumber() + ".pdf", pdfBytes));
        }
        return billingRepository.save(billing);
    }

    /**
     * Self-heals a billing whose PDF never attached (e.g. a storage outage during sync left
     * {@code pdf_url} null). Runs on the billing-event executor so it cannot race a concurrent
     * sync of the same billing, and re-enters via the self proxy for a writable transaction.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MonthlyBilling ensurePdfForBilling(Long id) {
        return runOnBillingThread(() -> self.getObject().ensurePdfForBillingInTx(id));
    }

    @Transactional
    public MonthlyBilling ensurePdfForBillingInTx(Long id) {
        var billing = billingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Billing not found: " + id));
        if (billing.getPdfUrl() != null && !billing.getPdfUrl().isBlank()) {
            return billing;   // already rendered
        }
        renderAndAttach(billing);
        var saved = billingRepository.save(billing);
        log.info("Lazily rendered PDF for monthly billing {}", billing.getBillingNumber());
        return saved;
    }

    @Override
    public int regenerateAllPdfs() {
        int count = 0;
        for (MonthlyBilling billing : billingRepository.findAll(null, null, null)) {
            var client = clients.findById(billing.getClientId()).orElse(null);
            if (client == null) {
                log.warn("Skipping PDF refresh for billing {} — client {} not found",
                        billing.getBillingNumber(), billing.getClientId());
                continue;
            }
            // Layout-only re-render from the stored billing record — totals/period/status unchanged,
            // so it is safe even for ISSUED/PAID billings. department_name is denormalized on the row.
            var pdfBytes = pdf.renderMonthlyBilling(toDocument(billing, client));
            var key = storage.store("billings/" + billing.getBillingNumber() + ".pdf", pdfBytes);
            billing.attachPdf(key);
            billingRepository.save(billing);
            count++;
        }
        log.info("Refreshed {} monthly-billing PDFs", count);
        return count;
    }

    @Override
    public void sync(Long orderId) {
        var snapshot = deliveredOrders.findBillableOrder(orderId);    // empty if canceled / gone
        var existing = billingRepository.findAllByOrderLine(orderId); // its current billing(s), if any

        // Canceled / removed → drop from every billing it is on (only while still DRAFT). A line on
        // a frozen (ISSUED/PAID) bill cannot be retracted — the issued document is final.
        if (snapshot.isEmpty()) {
            for (var b : existing) {
                if (b.getStatus() == BillingStatus.DRAFT) {
                    dropOrderFrom(b, orderId);
                }
            }
            return;
        }

        var o = snapshot.get();
        var client = clients.findById(o.clientId())
                .orElseThrow(() -> new NotFoundException("Client not found: " + o.clientId()));
        var portions = portionsOf(o, client.perDepartment());
        var cycle = client.cycle();
        var naturalYm = cycle.periodOf(o.orderDate());

        // Reconcile every department the order currently touches *plus* every department it is
        // already billed on — so a department an edit emptied out of the order is reconciled too
        // . (Its line dropped from a DRAFT, or credited forward off a frozen bill.)
        var deptIds = new LinkedHashSet<Long>();
        portions.forEach(p -> deptIds.add(p.departmentId()));
        existing.forEach(b -> deptIds.add(b.getDepartmentId()));

        for (var deptId : deptIds) {
            // The order's portion for this department, if it still touches it (absent = emptied out).
            var portion = portions.stream()
                    .filter(p -> Objects.equals(p.departmentId(), deptId))
                    .findFirst();
            var desired = portion.map(Portion::subtotal).orElse(BigDecimal.ZERO);
            var deptName = portion.map(Portion::departmentName).orElse(null);

            // Order-date correction cleanup: when this order carries no frozen line for the
            // department, it must live only on its natural-period bill. A SUPER_ADMIN order-date
            // change can leave a stale DRAFT line in the *old* period — drop it so the order isn't
            // double-billed. Guarded by "no frozen bill for this dept" so it never touches a
            // legitimate rolled-forward adjustment (KI-3), which only exists alongside a frozen bill.
            boolean onFrozenForDept = existing.stream()
                    .filter(b -> Objects.equals(b.getDepartmentId(), deptId))
                    .anyMatch(b -> b.getStatus() != BillingStatus.DRAFT);
            if (!onFrozenForDept) {
                existing.stream()
                        .filter(b -> Objects.equals(b.getDepartmentId(), deptId))
                        .filter(b -> b.getStatus() == BillingStatus.DRAFT)
                        .filter(b -> !(b.getPeriodYear() == naturalYm.getYear()
                                && b.getPeriodMonth() == naturalYm.getMonthValue()))
                        .toList()
                        .forEach(b -> dropOrderFrom(b, orderId));
            }

            // The bill in the order's natural period for this department, if the order is on one.
            var naturalBill = existing.stream()
                    .filter(b -> Objects.equals(b.getDepartmentId(), deptId))
                    .filter(b -> b.getPeriodYear() == naturalYm.getYear()
                            && b.getPeriodMonth() == naturalYm.getMonthValue())
                    .findFirst();

            if (naturalBill.isPresent() && naturalBill.get().getStatus() != BillingStatus.DRAFT) {
                // KI-3 (option b): the order's natural-period bill is frozen (ISSUED/PAID), so its
                // line is immutable. Reconcile by rolling the DELTA into the next open DRAFT — the
                // edit's money is preserved, not silently lost.
                //
                // KI-9: the delta must net against EVERY frozen line for this order in this
                // department — the natural bill *plus* any intervening period an earlier edit's
                // adjustment rolled into that has since itself been issued — not just the natural
                // bill. Subtracting only the natural amount would re-bill a now-frozen prior
                // adjustment a second time. `existing` already lists every bill holding this order.
                if (deptName == null) {
                    deptName = naturalBill.get().getDepartmentName();
                }
                var frozenBilled = existing.stream()
                        .filter(b -> Objects.equals(b.getDepartmentId(), deptId))
                        .filter(b -> b.getStatus() != BillingStatus.DRAFT)
                        .map(b -> lineSubtotal(b, orderId))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                var delta = desired.subtract(frozenBilled);
                reconcileAdjustment(client, cycle, deptId, deptName, o, existing, naturalYm, delta);
            } else if (naturalBill.isPresent()) {
                // Natural-period DRAFT → upsert the full current amount, or drop the line if an edit
                // moved all of this department's items out of the order.
                var b = naturalBill.get();
                b.repositionPeriod(cycle.startOf(naturalYm), cycle.endOf(naturalYm));
                if (desired.signum() == 0) {
                    dropOrderFrom(b, orderId);
                } else {
                    b.upsertLine(MonthlyBillingLine.of(o.orderId(), o.orderNumber(), o.orderDate(), desired));
                    renderAndAttach(b);
                    billingRepository.save(b);
                }
            } else if (desired.signum() != 0) {
                // Not yet billed on this department → resolve the open DRAFT (rolling forward if the
                // natural month is already closed) and upsert the full amount.
                var target = resolveTargetDraft(client, cycle, deptId, deptName, o.orderDate());
                target.upsertLine(MonthlyBillingLine.of(o.orderId(), o.orderNumber(), o.orderDate(), desired));
                renderAndAttach(target);
                billingRepository.save(target);
            }
        }
    }

    /**
     * Re-runs {@link #sync} for the client's billable orders from {@code from} onward. Dispatched
     * onto the single-thread billing executor and awaited, so it is serialized with the async
     * order→billing sync exactly like the manual rebuild (KI-4).
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int resyncClient(Long clientId, LocalDate from) {
        return runOnBillingThread(() -> self.getObject().resyncClientInternal(clientId, from));
    }

    @Transactional
    public int resyncClientInternal(Long clientId, LocalDate from) {
        clients.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found: " + clientId));
        var orders = deliveredOrders.findBillableOrders(clientId, from, LocalDate.now());
        for (var order : orders) {
            sync(order.orderId());
        }
        log.info("Re-synced {} orders for client {} from {}", orders.size(), clientId, from);
        return orders.size();
    }

    // ── helpers ──

    /**
     * Runs {@code work} on the single-thread {@code billingEventExecutor} and blocks for its result,
     * so a manual rebuild is serialized with the async sync (KI-4). Runtime exceptions (the empty-period /
     * regeneration-rejected / not-found guards) propagate to the caller unchanged.
     */
    private <T> T runOnBillingThread(Supplier<T> work) {
        var future = new CompletableFuture<T>();
        billingEventExecutor.execute(() -> {
            try {
                future.complete(work.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while generating billing", e);
        } catch (ExecutionException e) {
            switch (e.getCause()) {
                case RuntimeException re -> throw re;
                case Error err -> throw err;
                case null, default -> throw new IllegalStateException("Billing generation failed", e.getCause());
            }
        }
    }

    /**
     * Rolls a billing delta for an order whose natural-period bill is frozen into the next open
     * DRAFT period (KI-3 option b). A zero delta clears any stale adjustment line; a non-zero delta
     * is upserted as a single line keyed by the order, so repeated edits always reflect the
     * cumulative difference from the frozen amount rather than double-counting.
     */
    private void reconcileAdjustment(ClientInfo client, BillingCycle cycle, Long deptId, String deptName,
                                     DeliveredOrder o, List<MonthlyBilling> existing, YearMonth naturalYm,
                                     BigDecimal delta) {
        if (delta.signum() == 0) {
            existing.stream()
                    .filter(b -> Objects.equals(b.getDepartmentId(), deptId))
                    .filter(b -> b.getStatus() == BillingStatus.DRAFT)
                    .filter(b -> !(b.getPeriodYear() == naturalYm.getYear()
                            && b.getPeriodMonth() == naturalYm.getMonthValue()))
                    .findFirst()
                    .ifPresent(b -> dropOrderFrom(b, o.orderId()));
            return;
        }
        var target = resolveTargetDraft(client, cycle, deptId, deptName, o.orderDate());
        target.upsertLine(MonthlyBillingLine.of(o.orderId(), o.orderNumber(), o.orderDate(), delta));
        renderAndAttach(target);
        billingRepository.save(target);
    }

    /**
     * The subtotal currently billed for an order on a given billing (zero if it has no such line).
     */
    private static BigDecimal lineSubtotal(MonthlyBilling billing, Long orderId) {
        return billing.getLines().stream()
                .filter(l -> l.orderId().equals(orderId))
                .map(MonthlyBillingLine::subtotal)
                .findFirst().orElse(BigDecimal.ZERO);
    }

    /**
     * A single department's share of one order (departmentId null for COMBINED clients).
     */
    private record Portion(Long departmentId, String departmentName, BigDecimal subtotal) {
    }

    /**
     * Splits a delivered order into per-department portions. COMBINED clients yield a single
     * department-less portion for the whole order total; PER_DEPARTMENT clients yield one
     * portion per department the order's line items touch, summing those lines' subtotals.
     */
    private List<Portion> portionsOf(DeliveredOrder o, boolean perDepartment) {
        if (!perDepartment) {
            return List.of(new Portion(null, null, o.total()));
        }
        Map<Long, BigDecimal> subtotalByDept = new LinkedHashMap<>();
        Map<Long, String> nameByDept = new LinkedHashMap<>();
        for (var line : o.lines()) {
            subtotalByDept.merge(line.departmentId(), line.subtotal(), BigDecimal::add);
            nameByDept.putIfAbsent(line.departmentId(), line.departmentName());
        }
        return subtotalByDept.entrySet().stream()
                .map(e -> new Portion(e.getKey(), nameByDept.get(e.getKey()), e.getValue()))
                .toList();
    }

    /**
     * Removes an order's line from a DRAFT billing, deleting the billing if it becomes empty.
     */
    private void dropOrderFrom(MonthlyBilling billing, Long orderId) {
        billing.removeLine(orderId);
        if (billing.isEmpty()) {
            billingRepository.deleteById(billing.getId());
        } else {
            renderAndAttach(billing);
            billingRepository.save(billing);
        }
    }

    /**
     * The open DRAFT billing for the (client, department, period). Rolls forward one period at a
     * time while the natural period is already ISSUED/PAID (a closed period), and starts a fresh
     * empty DRAFT if none exists yet.
     *
     * <p>A period keeps its {@link YearMonth} identity even on a cut-off cycle (it is labelled by
     * the month it ends in), so "the next period" is still {@code plusMonths(1)} — only the
     * order-date → period mapping and the stored date range come from the cycle.
     */
    private MonthlyBilling resolveTargetDraft(ClientInfo client, BillingCycle cycle, Long departmentId,
                                              String departmentName, LocalDate orderDate) {
        var ym = cycle.periodOf(orderDate);
        for (int i = 0; i < 60; i++) {   // bounded; the current period is always open
            var existing = billingRepository.findExisting(client.id(), departmentId, ym.getYear(), ym.getMonthValue());
            if (existing.isEmpty()) {
                var number = buildBillingNumber(client.clientCode(), ym.getYear(), ym.getMonthValue(),
                        departmentId, departmentName);
                return MonthlyBilling.startNew(number, client.id(), departmentId, departmentName,
                        ym.getYear(), ym.getMonthValue(), cycle.startOf(ym), cycle.endOf(ym), LocalDate.now());
            }
            if (existing.get().getStatus() == BillingStatus.DRAFT) {
                // A DRAFT follows the client's current cycle (ISSUE freezes it), so a draft created
                // before a cut-off change is realigned here rather than left claiming the old range.
                var draft = existing.get();
                draft.repositionPeriod(cycle.startOf(ym), cycle.endOf(ym));
                return draft;
            }
            ym = ym.plusMonths(1);   // closed (ISSUED/PAID) — roll into the next period
        }
        throw new IllegalStateException("No open billing period found for client " + client.id());
    }

    /**
     * Renders the billing PDF, stores it, and attaches the key.
     */
    private void renderAndAttach(MonthlyBilling billing) {
        var client = clients.findById(billing.getClientId())
                .orElseThrow(() -> new NotFoundException("Client not found: " + billing.getClientId()));
        var pdfBytes = pdf.renderMonthlyBilling(toDocument(billing, client));
        var key = storage.store("billings/" + billing.getBillingNumber() + ".pdf", pdfBytes);
        billing.attachPdf(key);
    }

    private MonthlyBilling buildAndSave(ClientInfo client, Long departmentId, String departmentName,
                                        YearMonth period, LocalDate periodStart, LocalDate periodEnd,
                                        List<MonthlyBillingLine> lines) {
        int year = period.getYear();
        int month = period.getMonthValue();
        billingRepository.findExisting(client.id(), departmentId, year, month).ifPresent(existing -> {
            if (existing.getStatus() != BillingStatus.DRAFT) {
                throw new IllegalArgumentException(
                        "A %s billing already exists for %s %d-%02d and cannot be regenerated"
                                .formatted(existing.getStatus(), client.clientCode(), year, month));
            }
            billingRepository.deleteById(existing.getId());
        });

        var billingNumber = buildBillingNumber(client.clientCode(), year, month, departmentId, departmentName);
        var billing = MonthlyBilling.generate(billingNumber, client.id(), departmentId, departmentName, year, month,
                periodStart, periodEnd, LocalDate.now(), lines);

        var pdfBytes = pdf.renderMonthlyBilling(toDocument(billing, client));
        var key = storage.store("billings/" + billingNumber + ".pdf", pdfBytes);
        billing.attachPdf(key);

        return billingRepository.save(billing);
    }

    private String buildBillingNumber(String clientCode, int year, int month, Long departmentId, String departmentName) {
        var base = "%s%s-%04d%02d".formatted(BILLING_NUMBER_PREFIX, clientCode, year, month);
        return departmentId == null ? base
                : base + "-" + BillingFormats.departmentAbbrev(departmentName, departmentId);
    }

    /**
     * The company header for a billing: while DRAFT it is composed live — the current letterhead
     * plus the bank account this client is currently assigned to — so it follows both profile
     * edits and a reassignment. Once ISSUED/PAID it is the frozen snapshot. Falls back to live if
     * an issued snapshot is somehow missing, so the letterhead is never blank.
     */
    private CompanyHeader companyHeaderFor(MonthlyBilling billing, ClientInfo client) {
        if (billing.getStatus() == BillingStatus.DRAFT || billing.getCompanyName() == null) {
            var c = companyProfile.current();
            var bank = companyProfile.bankAccount(client.bankAccountId());
            return new CompanyHeader(c.companyName(), c.address(), c.phone(), bank.beneficiary(),
                    bank.bankName(), bank.accountNumber(), bank.accountHolder());
        }
        return new CompanyHeader(billing.getCompanyName(), billing.getCompanyAddress(), billing.getCompanyPhone(),
                billing.getBankBeneficiary(), billing.getBankName(), billing.getBankAccount(), billing.getBankHolder());
    }

    private MonthlyBillingDocument toDocument(MonthlyBilling billing, ClientInfo client) {
        return new MonthlyBillingDocument(
                companyHeaderFor(billing, client),
                billing.getBillingNumber(),
                client.name(),
                billing.getDepartmentName() == null ? "" : billing.getDepartmentName(),
                BillingFormats.shortDateYy(billing.getInvoiceDate()),
                BillingFormats.PAYMENT_TERMS,
                BillingFormats.periodDescription(billing.getPeriodStart(), billing.getPeriodEnd()),
                BillingFormats.money(billing.getTotal()),
                BillingFormats.terbilang(billing.getTotal()));
    }
}