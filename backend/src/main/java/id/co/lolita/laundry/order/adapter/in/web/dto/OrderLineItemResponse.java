package id.co.lolita.laundry.order.adapter.in.web.dto;

import id.co.lolita.laundry.order.domain.OrderLineItem;

import java.math.BigDecimal;

public record OrderLineItemResponse(
        Long id,
        Long itemId,
        BigDecimal quantity,
        BigDecimal priceAtOrder,
        BigDecimal subtotal,
        Long departmentId
) {
    public static OrderLineItemResponse from(OrderLineItem li) {
        return new OrderLineItemResponse(li.id(), li.itemId(), li.quantity(),
                li.priceAtOrder(), li.subtotal(), li.departmentId());
    }
}