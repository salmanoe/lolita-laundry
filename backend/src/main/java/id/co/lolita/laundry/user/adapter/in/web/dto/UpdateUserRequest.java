package id.co.lolita.laundry.user.adapter.in.web.dto;

import id.co.lolita.laundry.user.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Edit a user's display name and role. The Auth0 {@code sub} is immutable (it is the identity).
 */
public record UpdateUserRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotNull Role role) {
}