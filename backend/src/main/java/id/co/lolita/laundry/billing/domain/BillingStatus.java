package id.co.lolita.laundry.billing.domain;

/**
 * Monthly billing lifecycle. Transitions are strictly one-way and single-step:
 * {@code DRAFT → ISSUED → PAID}. A DRAFT may still be regenerated; once ISSUED it is
 * locked (it has been sent to the client).
 */
public enum BillingStatus {
    DRAFT,
    ISSUED,
    PAID;

    /** True only if {@code target} is the immediate successor of this status. */
    public boolean canTransitionTo(BillingStatus target) {
        return target != null && target.ordinal() == this.ordinal() + 1;
    }
}