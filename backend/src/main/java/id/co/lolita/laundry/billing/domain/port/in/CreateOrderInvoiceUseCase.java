package id.co.lolita.laundry.billing.domain.port.in;

import id.co.lolita.laundry.billing.domain.OrderInvoice;

/**
 * Generates the per-order invoice for a delivered order. Invoked by the billing module's
 * event adapter in response to {@code OrderDeliveredEvent}. Idempotent — a second call for
 * an order that already has an invoice is a no-op, so event redelivery is safe.
 */
public interface CreateOrderInvoiceUseCase {

    void createForDeliveredOrder(Long orderId);

    /**
     * Returns the order's invoice, lazily rendering and storing its PDF if it has none yet.
     * Normal invoices get their PDF at delivery; invoices created without one (e.g. a SQL
     * backfill of legacy delivered orders) are healed on first view. Throws if the order has
     * no invoice at all. Idempotent: a no-op once the PDF exists.
     */
    OrderInvoice ensurePdfForOrder(Long orderId);

    /**
     * Re-renders and re-stores the PDF for every existing invoice (layout-only — amounts and
     * numbers are unchanged), so a template change is applied to documents already generated.
     * Returns how many were refreshed. Used by the bulk "Perbarui Semua PDF" admin action.
     */
    int regenerateAllPdfs();
}