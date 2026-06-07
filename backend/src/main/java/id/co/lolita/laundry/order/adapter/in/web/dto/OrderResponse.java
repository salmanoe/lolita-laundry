package id.co.lolita.laundry.order.adapter.in.web.dto;

import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Full order detail, including line items.
 */
public record OrderResponse(
        Long id,
        String orderNumber,
        Long clientId,
        LocalDate orderDate,
        LocalDate dueDate,
        OrderStatus status,
        BigDecimal pricingMultiplier,
        String submittedByName,
        String notes,
        Long createdByUserId,
        Instant createdAt,
        BigDecimal total,
        List<OrderLineItemResponse> lineItems
) {
    public static OrderResponse from(Order o) {
        return new OrderResponse(
                o.getId(), o.getOrderNumber(), o.getClientId(),
                o.getOrderDate(), o.getDueDate(), o.getStatus(), o.getPricingMultiplier(),
                o.getSubmittedByName(), o.getNotes(), o.getCreatedByUserId(),
                o.getCreatedAt(),
                o.total(),
                o.getLineItems().stream().map(OrderLineItemResponse::from).toList()
        );
    }
}