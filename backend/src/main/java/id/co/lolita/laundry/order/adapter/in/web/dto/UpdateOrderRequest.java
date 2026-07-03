package id.co.lolita.laundry.order.adapter.in.web.dto;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

/**
 * Edits a RECEIVED or PROCESSING order. {@code items} null/empty leaves line items unchanged.
 * {@code orderDate} and {@code treatment} are honored only for SUPER_ADMIN callers (ignored
 * otherwise, in the controller); null leaves each unchanged. Setting {@code treatment} flips
 * Reguler ⇄ Treatment (×2) and re-prices every line.
 */
public record UpdateOrderRequest(
        LocalDate orderDate,
        Boolean treatment,
        LocalDate dueDate,
        String notes,
        @Valid List<OrderLineRequest> items
) {
}
