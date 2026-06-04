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
    private final String fullName;
    private final Role role;
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

}
