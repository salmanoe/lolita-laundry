package id.co.lolita.laundry.order.adapter.in.web.dto;

import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Lightweight order row for list views (no line items).
 */
public record OrderSummaryResponse(
        Long id,
        String orderNumber,
        Long clientId,
        LocalDate orderDate,
        LocalDate dueDate,
        OrderStatus status,
        BigDecimal pricingMultiplier,
        String submittedByName,
        BigDecimal total,
        Instant createdAt
) {
    public static OrderSummaryResponse from(Order o) {
        return new OrderSummaryResponse(
                o.getId(), o.getOrderNumber(), o.getClientId(),
                o.getOrderDate(), o.getDueDate(), o.getStatus(), o.getPricingMultiplier(),
                o.getSubmittedByName(), o.total(), o.getCreatedAt()
        );
    }
}
