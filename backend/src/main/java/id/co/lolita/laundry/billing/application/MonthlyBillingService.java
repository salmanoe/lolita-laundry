package id.co.lolita.laundry.billing.application;

import id.co.lolita.laundry.billing.domain.BillingStatus;
import id.co.lolita.laundry.billing.domain.MonthlyBilling;
import id.co.lolita.laundry.billing.domain.MonthlyBillingLine;
import id.co.lolita.laundry.billing.domain.port.in.GenerateMonthlyBillingUseCase;
import id.co.lolita.laundry.billing.domain.port.in.UpdateBillingStatusUseCase;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway.ClientInfo;
import id.co.lolita.laundry.billing.domain.port.out.BillingStoragePort;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway.DeliveredOrder;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.BillingOrderRow;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.MonthlyBillingDocument;
import id.co.lolita.laundry.billing.domain.port.out.MonthlyBillingRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
class MonthlyBillingService implements GenerateMonthlyBillingUseCase, UpdateBillingStatusUseCase {

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

        var delivered = deliveredOrders.findDeliveredOrders(command.clientId(), command.year(), command.month());
        if (delivered.isEmpty()) {
            throw new IllegalArgumentException("No delivered orders for client %d in %d-%02d"
                    .formatted(command.clientId(), command.year(), command.month()));
        }

        List<MonthlyBilling> results = new ArrayList<>();
        if (client.perDepartment()) {
            for (var group : groupByDepartment(delivered).entrySet()) {
                var orders = group.getValue();
                results.add(buildAndSave(client, group.getKey(), orders.getFirst().departmentName(),
                        command.year(), command.month(), orders));
            }
        } else {
            results.add(buildAndSave(client, null, null, command.year(), command.month(), delivered));
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

    // ── helpers ──

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
        var billing = MonthlyBilling.generate(billingNumber, client.id(), departmentId, year, month,
                LocalDate.now(), lines);

        var pdfBytes = pdf.renderMonthlyBilling(toDocument(billing, client, departmentName));
        var key = storage.store("billings/" + billingNumber + ".pdf", pdfBytes);
        billing.attachPdf(key);

        return billingRepository.save(billing);
    }

    private String buildBillingNumber(String clientCode, int year, int month, Long departmentId, String departmentName) {
        var base = "%s%s-%04d%02d".formatted(BILLING_NUMBER_PREFIX, clientCode, year, month);
        return departmentId == null ? base
                : base + "-" + BillingFormats.departmentAbbrev(departmentName, departmentId);
    }

    private MonthlyBillingDocument toDocument(MonthlyBilling billing, ClientInfo client, String departmentName) {
        List<BillingOrderRow> rows = billing.getLines().stream()
                .map(l -> new BillingOrderRow(
                        l.orderNumber(),
                        BillingFormats.shortDate(l.orderDate()),
                        BillingFormats.money(l.subtotal())))
                .toList();

        return new MonthlyBillingDocument(
                billing.getBillingNumber(),
                BillingFormats.periodLabel(billing.getPeriodYear(), billing.getPeriodMonth()),
                client.name(),
                client.clientCode(),
                departmentName == null ? "" : departmentName,
                BillingFormats.longDate(billing.getInvoiceDate()),
                rows,
                BillingFormats.money(billing.getTotal()));
    }
}