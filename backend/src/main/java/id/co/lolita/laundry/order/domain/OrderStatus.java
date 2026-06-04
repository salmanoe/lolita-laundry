package id.co.lolita.laundry.order.domain;

/**
 * Order lifecycle status. Transitions are strictly one-way and single-step:
 * {@code RECEIVED → PROCESSING → DONE → DELIVERED}.
 */
public enum OrderStatus {
    RECEIVED,
    PROCESSING,
    DONE,
    DELIVERED;

    /**
     * True only if {@code target} is the immediate successor of this status.
     */
    public boolean canAdvanceTo(OrderStatus target) {
        return target != null && target.ordinal() == this.ordinal() + 1;
    }
}
