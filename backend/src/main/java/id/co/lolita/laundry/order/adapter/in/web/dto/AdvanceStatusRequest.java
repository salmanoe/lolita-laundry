package id.co.lolita.laundry.order.adapter.in.web.dto;

import id.co.lolita.laundry.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

/** Advances an order to the given status (one step; never DELIVERED — use the delivery endpoint). */
public record AdvanceStatusRequest(
        @NotNull OrderStatus status,
        String notes
) {
}
