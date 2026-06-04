package id.co.lolita.laundry.order.domain;

import java.time.Instant;

/**
 * An audit entry recording one status transition of an order.
 * The initial entry for a new order has {@code fromStatus = null} and {@code toStatus = RECEIVED}.
 *
 * @param fromStatus      nullable — null for the initial RECEIVED entry
 * @param changedByUserId nullable — null for public submissions
 */
public record OrderStatusHistory(Long id, Long orderId, OrderStatus fromStatus, OrderStatus toStatus,
                                 Long changedByUserId, Instant changedAt, String notes) {

    public static OrderStatusHistory record(Long orderId, OrderStatus fromStatus, OrderStatus toStatus,
                                            Long changedByUserId, String notes, Instant changedAt) {
        return new OrderStatusHistory(null, orderId, fromStatus, toStatus, changedByUserId, changedAt, notes);
    }
}