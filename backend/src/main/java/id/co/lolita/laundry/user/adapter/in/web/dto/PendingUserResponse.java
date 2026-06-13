package id.co.lolita.laundry.user.adapter.in.web.dto;

import id.co.lolita.laundry.user.domain.PendingUser;

import java.time.Instant;

/**
 * A self-registered identity awaiting approval, for the SUPER_ADMIN's "Permintaan Akses" list.
 */
public record PendingUserResponse(Long id, String auth0Sub, String email, String fullName,
                                  Instant requestedAt) {
    public static PendingUserResponse from(PendingUser p) {
        return new PendingUserResponse(p.id(), p.auth0Sub(), p.email(), p.fullName(),
                p.requestedAt());
    }
}
