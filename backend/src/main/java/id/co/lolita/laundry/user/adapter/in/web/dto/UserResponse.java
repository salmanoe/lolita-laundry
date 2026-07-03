package id.co.lolita.laundry.user.adapter.in.web.dto;

import id.co.lolita.laundry.user.domain.User;

import java.time.Instant;

/**
 * A Lolita user row for the owner's user-management screen.
 */
public record UserResponse(Long id, String auth0Sub, String email, String fullName, String role,
                           boolean active, Instant createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getAuth0Sub(), u.getEmail(), u.getFullName(),
                u.getRole().name(), u.isActive(), u.getCreatedAt());
    }
}