package id.co.lolita.laundry.billing.domain.port.in;

/**
 * Generates the per-order invoice for a delivered order. Invoked by the billing module's
 * event adapter in response to {@code OrderDeliveredEvent}. Idempotent — a second call for
 * an order that already has an invoice is a no-op, so event redelivery is safe.
 */
public interface CreateOrderInvoiceUseCase {

    void createForDeliveredOrder(Long orderId);
}