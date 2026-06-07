package id.co.lolita.laundry.catalog.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateItemRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long unitId,
        @NotNull Boolean active
) {
}