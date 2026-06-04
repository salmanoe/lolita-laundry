package id.co.lolita.laundry.client.domain;

import lombok.Getter;

/**
 * A billing department within a client (e.g. PBS: Room Linen, Uniform/Guest, F&B Linen).
 * Only relevant when {@code Client.billingMode = PER_DEPARTMENT}.
 */
@Getter
public class Department {

    private final Long id;
    private final Long clientId;
    private String name;
    private boolean active;

    public Department(Long id, Long clientId, String name, boolean active) {
        this.id = id;
        this.clientId = clientId;
        this.name = name;
        this.active = active;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

}
