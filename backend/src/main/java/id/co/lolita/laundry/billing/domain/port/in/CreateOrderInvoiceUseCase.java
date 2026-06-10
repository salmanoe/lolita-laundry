package id.co.lolita.laundry.billing.domain.port.in;

import id.co.lolita.laundry.billing.domain.OrderInvoice;

/**
 * Generates the per-order invoice. The {@code OrderDeliveredEvent} listener renders the frozen
 * invoice at delivery; staff can also view a live preview from RECEIVED onward. Idempotent —
 * one invoice row per order, so event redelivery and repeated views never duplicate it.
 */
public interface CreateOrderInvoiceUseCase {

    void createForDeliveredOrder(Long orderId);

    /**
     * Returns the order's invoice for viewing, creating or refreshing it on demand. While the
     * order is still open (RECEIVED/PROCESSING/DONE) this re-renders a live preview reflecting
     * the latest order/company state; once the order is DELIVERED the invoice is frozen and
     * returned as-is. Throws {@code NotFoundException} for an unknown or canceled order.
     */
    OrderInvoice prepareInvoiceForOrder(Long orderId);

    /**
     * Re-renders and re-stores the PDF for every existing invoice (layout-only — amounts and
     * numbers are unchanged), so a template change is applied to documents already generated.
     * Returns how many were refreshed. Used by the bulk "Perbarui Semua PDF" admin action.
     */
    int regenerateAllPdfs();
}