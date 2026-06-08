package id.co.lolita.laundry.billing.application;

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
import id.co.lolita.laundry.billing.domain.port.out.CompanyProfileGateway.CompanyInfo;
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

        var billable = deliveredOrders.findBillableOrders(command.clientId(), command.year(), command.month());
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
                        command.year(), command.month(), entry.getValue()));
            }
        } else {
            var lines = billable.stream()
                    .map(o -> MonthlyBillingLine.of(o.orderId(), o.orderNumber(), o.orderDate(), o.total()))
                    .toList();
            results.add(buildAndSave(client, null, null, command.year(), command.month(), lines));
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
            // Freeze the company letterhead + bank details onto the billing at issue time, then
            // re-render so the issued PDF is self-contained and immune to later profile changes.
            var c = companyProfile.current();
            billing.captureCompany(c.companyName(), c.address(), c.phone(), c.bankBeneficiary(),
                    c.bankName(), c.bankAccount(), c.bankHolder());
            var client = clients.findById(billing.getClientId())
                    .orElseThrow(() -> new NotFoundException("Client not found: " + billing.getClientId()));
            var pdfBytes = pdf.renderMonthlyBilling(toDocument(billing, client));
            billing.attachPdf(storage.store("billings/" + billing.getBillingNumber() + ".pdf", pdfBytes));
        }
        return billingRepository.save(billing);
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
        var naturalYm = YearMonth.of(o.orderDate().getYear(), o.orderDate().getMonthValue());

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

            // The bill in the order's natural period for this department, if the order is on one.
            var naturalBill = existing.stream()
                    .filter(b -> Objects.equals(b.getDepartmentId(), deptId))
                    .filter(b -> b.getPeriodYear() == naturalYm.getYear()
                            && b.getPeriodMonth() == naturalYm.getMonthValue())
                    .findFirst();

            if (naturalBill.isPresent() && naturalBill.get().getStatus() != BillingStatus.DRAFT) {
                // KI-3 (option b): the order's natural-period bill is frozen (ISSUED/PAID), so its
                // line is immutable. Reconcile by rolling the DELTA against the frozen amount into
                // the next open DRAFT — the edit's money is preserved, not silently lost.
                if (deptName == null) {
                    deptName = naturalBill.get().getDepartmentName();
                }
                var delta = desired.subtract(lineSubtotal(naturalBill.get(), orderId));
                reconcileAdjustment(client, deptId, deptName, o, existing, naturalYm, delta);
            } else if (naturalBill.isPresent()) {
                // Natural-period DRAFT → upsert the full current amount, or drop the line if an edit
                // moved all of this department's items out of the order.
                var b = naturalBill.get();
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
                var target = resolveTargetDraft(client, deptId, deptName, o.orderDate());
                target.upsertLine(MonthlyBillingLine.of(o.orderId(), o.orderNumber(), o.orderDate(), desired));
                renderAndAttach(target);
                billingRepository.save(target);
            }
        }
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
    private void reconcileAdjustment(ClientInfo client, Long deptId, String deptName, DeliveredOrder o,
                                     List<MonthlyBilling> existing, YearMonth naturalYm, BigDecimal delta) {
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
        var target = resolveTargetDraft(client, deptId, deptName, o.orderDate());
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
     * The open DRAFT billing for the (client, department, period). Rolls forward one month at a
     * time while the natural period is already ISSUED/PAID (a closed month), and starts a fresh
     * empty DRAFT if none exists yet.
     */
    private MonthlyBilling resolveTargetDraft(ClientInfo client, Long departmentId, String departmentName,
                                              LocalDate orderDate) {
        var ym = YearMonth.of(orderDate.getYear(), orderDate.getMonthValue());
        for (int i = 0; i < 60; i++) {   // bounded; the current month is always open
            var existing = billingRepository.findExisting(client.id(), departmentId, ym.getYear(), ym.getMonthValue());
            if (existing.isEmpty()) {
                var number = buildBillingNumber(client.clientCode(), ym.getYear(), ym.getMonthValue(),
                        departmentId, departmentName);
                return MonthlyBilling.startNew(number, client.id(), departmentId, departmentName,
                        ym.getYear(), ym.getMonthValue(), LocalDate.now());
            }
            if (existing.get().getStatus() == BillingStatus.DRAFT) {
                return existing.get();
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
                                        int year, int month, List<MonthlyBillingLine> lines) {
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
                LocalDate.now(), lines);

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
     * The company header for a billing: the live profile while DRAFT (it follows profile edits),
     * the frozen snapshot once ISSUED/PAID. Falls back to live if an issued snapshot is somehow
     * missing, so the letterhead is never blank.
     */
    private CompanyHeader companyHeaderFor(MonthlyBilling billing) {
        if (billing.getStatus() == BillingStatus.DRAFT || billing.getCompanyName() == null) {
            return toHeader(companyProfile.current());
        }
        return new CompanyHeader(billing.getCompanyName(), billing.getCompanyAddress(), billing.getCompanyPhone(),
                billing.getBankBeneficiary(), billing.getBankName(), billing.getBankAccount(), billing.getBankHolder());
    }

    private static CompanyHeader toHeader(CompanyInfo c) {
        return new CompanyHeader(c.companyName(), c.address(), c.phone(), c.bankBeneficiary(),
                c.bankName(), c.bankAccount(), c.bankHolder());
    }

    private MonthlyBillingDocument toDocument(MonthlyBilling billing, ClientInfo client) {
        return new MonthlyBillingDocument(
                companyHeaderFor(billing),
                billing.getBillingNumber(),
                client.name(),
                billing.getDepartmentName() == null ? "" : billing.getDepartmentName(),
                BillingFormats.shortDateYy(billing.getInvoiceDate()),
                BillingFormats.PAYMENT_TERMS,
                BillingFormats.periodDescription(billing.getPeriodYear(), billing.getPeriodMonth()),
                BillingFormats.money(billing.getTotal()),
                BillingFormats.terbilang(billing.getTotal()));
    }
}