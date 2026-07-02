package id.co.lolita.laundry.billing.domain.port.in;

import id.co.lolita.laundry.billing.domain.MonthlyBilling;
import id.co.lolita.laundry.billing.domain.OrderInvoice;

import java.util.List;

/**
 * Read-side queries for billing history and PDFs.
 */
public interface GetBillingUseCase {

    /** Monthly billings, newest period first, optionally filtered by client/year/month. */
    List<MonthlyBilling> listBillings(Long clientId, Integer year, Integer month);

    MonthlyBilling getBilling(Long id);

    /**
     * Short-lived pre-signed URL to a monthly billing PDF. 404 if not yet rendered.
     * {@code download=true} forces a browser save (the "Unduh" action, reliable on mobile);
     * {@code false} serves it inline for preview.
     */
    String getBillingPdfUrl(Long id, boolean download);

    /** The per-order invoice for a delivered order. 404 if the order has no invoice yet. */
    OrderInvoice getInvoiceForOrder(Long orderId);

    /**
     * Short-lived pre-signed URL to a per-order invoice PDF. {@code download=true} forces a
     * browser save (reliable on mobile); {@code false} serves it inline for preview.
     */
    String getInvoicePdfUrlForOrder(Long orderId, boolean download);
}