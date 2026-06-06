package id.co.lolita.laundry.order.adapter.in.web.dto;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

/**
 * Edits a RECEIVED order. {@code items} null/empty leaves line items unchanged.
 */
public record UpdateOrderRequest(
        LocalDate dueDate,
        String notes,
        @Valid List<OrderLineRequest> items
) {
}
