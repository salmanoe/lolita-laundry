package id.co.lolita.laundry.billing.domain.port.in;

/**
 * Keeps the monthly billing in sync with one order's billable state. Invoked by the billing
 * module's listener for {@code OrderBillingSyncEvent} (order created / edited / canceled).
 *
 * <p>Idempotent: if the order is billable it is upserted into its period's DRAFT billing
 * (created on the first order of the month; rolled forward to the next open period if the
 * natural month is already ISSUED/PAID); if it is canceled/gone it is removed. Frozen
 * (ISSUED/PAID) billings are never modified.
 */
public interface SyncOrderBillingUseCase {

    void sync(Long orderId);
}