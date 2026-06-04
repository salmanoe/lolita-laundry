package id.co.lolita.laundry.order.adapter.in.web.dto;

import id.co.lolita.laundry.order.domain.port.in.OrderLineInput;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** One requested line on an order. Price is resolved server-side, never from the request. */
public record OrderLineRequest(
        @NotNull Long itemId,
        @NotNull @Positive BigDecimal quantity
) {
    public OrderLineInput toInput() {
        return new OrderLineInput(itemId, quantity);
    }
}
