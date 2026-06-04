package id.co.lolita.laundry.order.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Public order submission body (the token comes from the path).
 */
public record SubmitOrderRequest(
        @NotBlank String submittedByName,
        Long departmentId,
        boolean treatment,
        String notes,
        @NotEmpty @Valid List<OrderLineRequest> items
) {
}
