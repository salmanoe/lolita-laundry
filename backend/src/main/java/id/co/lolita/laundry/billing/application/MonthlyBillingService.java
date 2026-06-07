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
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway.DeliveredOrder;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.MonthlyBillingDocument;
import id.co.lolita.laundry.billing.domain.port.out.MonthlyBillingRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final InvoicePdfPort pdf;
    private final BillingStoragePort storage;

    @Override
    public List<MonthlyBilling> generate(GenerateCommand command) {
        var client = clients.findById(command.clientId())
                .orElseThrow(() -> new NotFoundException("Client not found: " + command.clientId()));

        var billable = deliveredOrders.findBillableOrders(command.clientId(), command.year(), command.month());
        if (billable.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada order untuk %s pada periode %s"
                    .formatted(client.name(), BillingFormats.periodLabel(command.year(), command.month())));
        }

        List<MonthlyBilling> results = new ArrayList<>();
        if (client.perDepartment()) {
            for (var group : groupByDepartment(billable).entrySet()) {
                var orders = group.getValue();
                results.add(buildAndSave(client, group.getKey(), orders.getFirst().departmentName(),
                        command.year(), command.month(), orders));
            }
        } else {
            results.add(buildAndSave(client, null, null, command.year(), command.month(), billable));
        }
        return results;
    }

    @Override
    public MonthlyBilling updateStatus(UpdateStatusCommand command) {
        var billing = billingRepository.findById(command.billingId())
                .orElseThrow(() -> new NotFoundException("Billing not found: " + command.billingId()));
        billing.advanceStatus(command.target());
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
        var snapshot = deliveredOrders.findBillableOrder(orderId);   // empty if canceled / gone
        var existing = billingRepository.findByOrderLine(orderId);   // its current billing, if any

        // Canceled / removed → drop from its billing (only while it is still a DRAFT).
        if (snapshot.isEmpty()) {
            existing.filter(b -> b.getStatus() == BillingStatus.DRAFT).ifPresent(b -> {
                b.removeLine(orderId);
                if (b.isEmpty()) {
                    billingRepository.deleteById(b.getId());
                } else {
                    renderAndAttach(b);
                    billingRepository.save(b);
                }
            });
            return;
        }

        var o = snapshot.get();
        var line = MonthlyBillingLine.of(o.orderId(), o.orderNumber(), o.orderDate(), o.total());

        // Already on a billing → re-price in place if it is still a DRAFT (else it is frozen).
        if (existing.isPresent()) {
            var b = existing.get();
            if (b.getStatus() == BillingStatus.DRAFT) {
                b.upsertLine(line);
                renderAndAttach(b);
                billingRepository.save(b);
            }
            return;
        }

        // Not yet billed → place into the target DRAFT period, rolled forward past closed months.
        var client = clients.findById(o.clientId())
                .orElseThrow(() -> new NotFoundException("Client not found: " + o.clientId()));
        var target = resolveTargetDraft(client, o);
        target.upsertLine(line);
        renderAndAttach(target);
        billingRepository.save(target);
    }

    // ── helpers ──

    /**
     * The open DRAFT billing for the order's (client, department, period). Rolls forward one
     * month at a time while the natural period is already ISSUED/PAID (a closed month), and
     * starts a fresh empty DRAFT if none exists yet.
     */
    private MonthlyBilling resolveTargetDraft(ClientInfo client, DeliveredOrder o) {
        Long departmentId = client.perDepartment() ? o.departmentId() : null;
        String departmentName = client.perDepartment() ? o.departmentName() : null;
        var ym = YearMonth.of(o.orderDate().getYear(), o.orderDate().getMonthValue());
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

    private Map<Long, List<DeliveredOrder>> groupByDepartment(List<DeliveredOrder> delivered) {
        Map<Long, List<DeliveredOrder>> byDept = new LinkedHashMap<>();
        for (var order : delivered) {
            byDept.computeIfAbsent(order.departmentId(), _ -> new ArrayList<>()).add(order);
        }
        return byDept;
    }

    private MonthlyBilling buildAndSave(ClientInfo client, Long departmentId, String departmentName,
                                        int year, int month, List<DeliveredOrder> orders) {
        billingRepository.findExisting(client.id(), departmentId, year, month).ifPresent(existing -> {
            if (existing.getStatus() != BillingStatus.DRAFT) {
                throw new IllegalArgumentException(
                        "A %s billing already exists for %s %d-%02d and cannot be regenerated"
                                .formatted(existing.getStatus(), client.clientCode(), year, month));
            }
            billingRepository.deleteById(existing.getId());
        });

        var lines = orders.stream()
                .map(o -> MonthlyBillingLine.of(o.orderId(), o.orderNumber(), o.orderDate(), o.total()))
                .toList();
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

    private MonthlyBillingDocument toDocument(MonthlyBilling billing, ClientInfo client) {
        return new MonthlyBillingDocument(
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