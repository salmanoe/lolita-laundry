package id.co.lolita.laundry.catalog.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateLookupRequest(
        @NotBlank @Size(max = 50) String displayName,
        int sortOrder,
        @NotNull Boolean active
) {
}