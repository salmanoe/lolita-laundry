package id.co.lolita.laundry.order.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** Staff-created order body. */
public record CreateOrderRequest(
        @NotNull Long clientId,
        Long departmentId,
        boolean treatment,
        LocalDate dueDate,
        @NotBlank String submittedByName,
        String notes,
        @NotEmpty @Valid List<OrderLineRequest> items
) {
}
