package id.co.lolita.laundry.billing.domain.port.in;

/**
 * Keeps the monthly billing in sync with one order's billable state. Invoked by the billing
 * module's listener for {@code OrderBillingSyncEvent} (order created / edited / canceled).
 *
 * <p>Idempotent: if the order is billable it is upserted into its period's DRAFT billing
 * (created on the first order of the period; rolled forward to the next open period if the
 * natural period is already ISSUED/PAID); if it is canceled/gone it is removed. Frozen
 * (ISSUED/PAID) billings are never modified.
 *
 * <p>A "period" is the client's billing cycle, not necessarily a calendar month — see
 * {@code BillingCycle}.
 */
public interface SyncOrderBillingUseCase {

    void sync(Long orderId);

    /**
     * Re-runs {@link #sync} for every billable order of a client from {@code from} onward, oldest
     * first, so the client's open DRAFT billings are rebuilt from current state.
     *
     * <p>Needed because {@code sync} is event-driven — it only fires when an order is created,
     * edited or canceled. Changing a client's <em>billing cycle</em> therefore does not, on its
     * own, re-home orders already sitting on a DRAFT: this does. Frozen (ISSUED/PAID) billings are
     * untouched, exactly as in the normal sync path (an edit's delta rolls forward instead).
     *
     * <p>Idempotent, and safe to run at any time. Returns how many orders were re-synced.
     */
    int resyncClient(Long clientId, java.time.LocalDate from);
}