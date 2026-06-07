package id.co.lolita.laundry.order.domain;

import java.util.List;

/**
 * Order lifecycle status. The happy path is strictly one-way and single-step:
 * {@code RECEIVED → PROCESSING → DONE → DELIVERED}. {@code CANCELLED} is a terminal off-ramp
 * (set via {@code Order.cancel()}), not part of the linear flow and never reached by
 * {@code advanceStatus}/{@code markDelivered}.
 */
public enum OrderStatus {
    RECEIVED,
    PROCESSING,
    DONE,
    DELIVERED,
    CANCELLED;

    /**
     * True only if {@code target} is the immediate successor on the linear flow. CANCELLED is
     * never a valid advance target or source (it is reached only through {@code Order.cancel()}).
     */
    public boolean canAdvanceTo(OrderStatus target) {
        if (target == null || this == CANCELLED || target == CANCELLED || target == DELIVERED) {
            return false;
        }
        return target.ordinal() == this.ordinal() + 1;
    }

    /**
     * The ordered statuses after this one, up to and including {@code DELIVERED}. For
     * {@code RECEIVED} that is {@code [PROCESSING, DONE, DELIVERED]}; for {@code DONE} just
     * {@code [DELIVERED]}; empty when already {@code DELIVERED}. Used to auto-stamp the steps a
     * delivery skipped when staff never advanced the status manually.
     */
    public List<OrderStatus> pathToDelivered() {
        return List.of(values()).subList(this.ordinal() + 1, DELIVERED.ordinal() + 1);
    }
}
