package id.co.lolita.laundry.order.adapter.in.web.dto;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

/**
 * Edits a RECEIVED order. {@code items} null/empty leaves line items unchanged.
 * {@code orderDate} is honored only for SUPER_ADMIN callers (ignored otherwise, in the
 * controller); null leaves the order date unchanged.
 */
public record UpdateOrderRequest(
        LocalDate orderDate,
        LocalDate dueDate,
        String notes,
        @Valid List<OrderLineRequest> items
) {
}
