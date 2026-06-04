package id.co.lolita.laundry.order.adapter.in.web.dto;

import id.co.lolita.laundry.order.domain.OrderStatus;
import id.co.lolita.laundry.order.domain.OrderStatusHistory;

import java.time.Instant;

public record StatusHistoryResponse(
        Long id,
        Long orderId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        Long changedByUserId,
        Instant changedAt,
        String notes
) {
    public static StatusHistoryResponse from(OrderStatusHistory h) {
        return new StatusHistoryResponse(h.id(), h.orderId(), h.fromStatus(), h.toStatus(),
                h.changedByUserId(), h.changedAt(), h.notes());
    }
}
