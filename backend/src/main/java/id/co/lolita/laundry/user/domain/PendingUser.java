package id.co.lolita.laundry.user.domain;

import java.time.Instant;

/**
 * A self-registered Auth0 identity awaiting SUPER_ADMIN approval.
 *
 * <p>Created on a user's first login (the frontend posts the verified {@code sub} plus the
 * profile {@code email}/{@code fullName} from the Auth0 ID token) when no {@code users} row
 * exists yet. It carries <strong>no role</strong> — the role is chosen at approval time, which
 * promotes this entry into a real {@link User} and clears the pending row.
 */
public record PendingUser(Long id, String auth0Sub, String email, String fullName, Instant requestedAt) {

    /**
     * Factory for a brand-new (unpersisted) pending registration. The {@code sub} is the
     * already-verified Auth0 identity of the caller; {@code email}/{@code fullName} are best-effort
     * profile hints the SUPER_ADMIN reviews before approving.
     */
    public static PendingUser of(String auth0Sub, String email, String fullName) {
        var sub = auth0Sub == null ? "" : auth0Sub.trim();
        if (sub.isEmpty()) {
            throw new IllegalArgumentException("Auth0 sub wajib diisi");
        }
        return new PendingUser(null, sub, trimToNull(email), trimToNull(fullName), Instant.now());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
