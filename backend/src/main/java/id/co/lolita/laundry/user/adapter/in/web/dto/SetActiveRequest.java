package id.co.lolita.laundry.user.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Toggle a user's active flag. Deactivating the last active OWNER is rejected server-side.
 */
public record SetActiveRequest(@NotNull Boolean active) {
}