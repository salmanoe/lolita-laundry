package id.co.lolita.laundry.billing.adapter.in.web.dto;

import id.co.lolita.laundry.billing.domain.BillingStatus;
import id.co.lolita.laundry.billing.domain.MonthlyBilling;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Monthly billing detail. {@code hasPdf} signals whether a rendered PDF is available without
 * exposing the storage key; the PDF is fetched via {@code GET /api/billing/{id}/pdf}.
 *
 * <p>{@code periodYear}/{@code periodMonth} identify and label the period; {@code periodStart}/
 * {@code periodEnd} are the calendar dates it actually covers. They differ only for a client on
 * a billing cut-off cycle (e.g. 26 Jul – 25 Aug is labelled Agustus).
 */
public record MonthlyBillingResponse(
        Long id, String billingNumber, Long clientId, Long departmentId, String departmentName,
        int periodYear, int periodMonth, LocalDate periodStart, LocalDate periodEnd,
        LocalDate invoiceDate, BigDecimal total,
        BillingStatus status, boolean hasPdf, String notes,
        List<MonthlyBillingLineResponse> lines
) {

    public static MonthlyBillingResponse from(MonthlyBilling b) {
        return new MonthlyBillingResponse(
                b.getId(), b.getBillingNumber(), b.getClientId(), b.getDepartmentId(), b.getDepartmentName(),
                b.getPeriodYear(), b.getPeriodMonth(), b.getPeriodStart(), b.getPeriodEnd(),
                b.getInvoiceDate(), b.getTotal(),
                b.getStatus(), b.getPdfUrl() != null && !b.getPdfUrl().isBlank(), b.getNotes(),
                b.getLines().stream().map(MonthlyBillingLineResponse::from).toList());
    }
}