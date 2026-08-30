package id.co.lolita.laundry.client.adapter.in.web.dto;

import id.co.lolita.laundry.client.domain.BillingMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 10) @Pattern(regexp = "[A-Z0-9]+", message = "Client code must be uppercase alphanumeric")
        String clientCode,
        @NotNull Long clientTypeId,
        @NotNull BillingMode billingMode,
        @Size(max = 100) String contactPerson,
        @Size(max = 20) String phone,
        String address,
        /* Which bank account the client's invoices are payable to. Null = the default account. */
        Long bankAccountId,
        /* Monthly billing cut-off day. Null = plain calendar month. Capped at 28 so the period
           end never has to be clamped to a short February. */
        @Min(1) @Max(28) Integer billingCycleDay
) {
}