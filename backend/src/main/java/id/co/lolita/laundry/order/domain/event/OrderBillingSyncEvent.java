package id.co.lolita.laundry.order.domain.event;

/**
 * Domain event signalling that an order's billable state changed and the client's monthly
 * billing should be re-synced. Published on order create (staff + public), edit (re-priced),
 * and cancel. Consumed cross-module by {@code billing}, whose listener upserts the order into
 * — or removes it from — the period's DRAFT billing.
 *
 * <p>Carries only the id; the billing module pulls the current snapshot (status, total,
 * client/department, order date) via its query gateway, so a single event type covers
 * "added/changed" and "cancelled" — the listener decides based on the order's current state.
 * Lives in the exposed {@code order::events} named interface.
 *
 * @param orderId the order whose billing membership must be re-evaluated
 */
public record OrderBillingSyncEvent(Long orderId) {
}
