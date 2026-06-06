package id.co.lolita.laundry.billing.adapter.in.web.dto;

import id.co.lolita.laundry.billing.domain.BillingStatus;
import id.co.lolita.laundry.billing.domain.MonthlyBilling;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Monthly billing detail. {@code hasPdf} signals whether a rendered PDF is available without
 * exposing the storage key; the PDF is fetched via {@code GET /api/billing/{id}/pdf}.
 */
public record MonthlyBillingResponse(
        Long id, String billingNumber, Long clientId, Long departmentId,
        int periodYear, int periodMonth, LocalDate invoiceDate, BigDecimal total,
        BillingStatus status, boolean hasPdf, String notes,
        List<MonthlyBillingLineResponse> lines
) {

    public static MonthlyBillingResponse from(MonthlyBilling b) {
        return new MonthlyBillingResponse(
                b.getId(), b.getBillingNumber(), b.getClientId(), b.getDepartmentId(),
                b.getPeriodYear(), b.getPeriodMonth(), b.getInvoiceDate(), b.getTotal(),
                b.getStatus(), b.getPdfUrl() != null && !b.getPdfUrl().isBlank(), b.getNotes(),
                b.getLines().stream().map(MonthlyBillingLineResponse::from).toList());
    }
}