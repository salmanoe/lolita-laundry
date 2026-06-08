package id.co.lolita.laundry.user.adapter.in.web.dto;

import id.co.lolita.laundry.user.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Register an existing Auth0 identity as a Lolita user. The owner creates the Auth0 account
 * first, then supplies its {@code sub} here (e.g. {@code auth0|abc123}).
 */
public record CreateUserRequest(
        @NotBlank @Size(max = 128) String auth0Sub,
        @NotBlank @Size(max = 100) String fullName,
        @NotNull Role role) {
}