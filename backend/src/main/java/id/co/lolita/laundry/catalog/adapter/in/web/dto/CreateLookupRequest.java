package id.co.lolita.laundry.catalog.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLookupRequest(
        @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9_]+", message = "Code must be uppercase letters, digits or underscore")
        String code,
        @NotBlank @Size(max = 50) String displayName,
        int sortOrder
) {
}