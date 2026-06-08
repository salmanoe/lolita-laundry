package id.co.lolita.laundry.user.domain;

import lombok.Getter;

import java.time.Instant;

/**
 * Represents a Lolita Laundry staff member (OWNER or STAFF).
 * Identity is delegated to Auth0 — no password stored here.
 * Hotel/client staff are not users; they access the system via tokenized public URLs.
 */
@Getter
public class User {

    private final Long id;
    private final String auth0Sub;
    private String fullName;
    private Role role;
    private boolean active;
    private final Instant createdAt;

    public User(Long id, String auth0Sub, String fullName, Role role, boolean active, Instant createdAt) {
        this.id = id;
        this.auth0Sub = auth0Sub;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    /**
     * Factory for a brand-new (unpersisted) user. Identity is the caller-supplied Auth0
     * {@code sub} — the owner creates the Auth0 account first, then registers it here.
     */
    public static User register(String auth0Sub, String fullName, Role role) {
        var sub = auth0Sub == null ? "" : auth0Sub.trim();
        if (sub.isEmpty()) {
            throw new IllegalArgumentException("Auth0 sub wajib diisi");
        }
        return new User(null, sub, requireName(fullName), role, true, Instant.now());
    }

    /**
     * Rename this user. Owner-only operation.
     */
    public void rename(String fullName) {
        this.fullName = requireName(fullName);
    }

    /**
     * Change this user's role. Owner-only operation.
     */
    public void changeRole(Role role) {
        this.role = role;
    }

    /**
     * Deactivate this user. Owner-only operation.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Reactivate a previously deactivated user.
     */
    public void activate() {
        this.active = true;
    }

    private static String requireName(String fullName) {
        var name = fullName == null ? "" : fullName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Nama wajib diisi");
        }
        return name;
    }

}
