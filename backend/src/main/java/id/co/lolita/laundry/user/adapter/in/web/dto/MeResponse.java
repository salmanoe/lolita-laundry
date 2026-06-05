package id.co.lolita.laundry.user.adapter.in.web.dto;

import id.co.lolita.laundry.user.domain.User;

/**
 * The currently authenticated Lolita user — drives role-aware routing in the frontend.
 */
public record MeResponse(Long id, String fullName, String role) {
    public static MeResponse from(User u) {
        return new MeResponse(u.getId(), u.getFullName(), u.getRole().name());
    }
}