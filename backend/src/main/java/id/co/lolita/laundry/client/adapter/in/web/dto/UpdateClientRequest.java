package id.co.lolita.laundry.client.adapter.in.web.dto;

import id.co.lolita.laundry.client.domain.BillingMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateClientRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long clientTypeId,
        @NotNull BillingMode billingMode,
        @Size(max = 100) String contactPerson,
        @Size(max = 20) String phone,
        String address
) {
}
