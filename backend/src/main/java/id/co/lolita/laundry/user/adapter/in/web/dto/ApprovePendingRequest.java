package id.co.lolita.laundry.user.adapter.in.web.dto;

import id.co.lolita.laundry.user.domain.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Approve a pending registration by assigning the role its new {@code users} row will carry.
 */
public record ApprovePendingRequest(@NotNull Role role) {
}
