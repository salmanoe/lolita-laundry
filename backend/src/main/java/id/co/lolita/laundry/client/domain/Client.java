package id.co.lolita.laundry.client.domain;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * A client (hotel, restaurant, etc.) served by Lolita Laundry.
 *
 * <p>Each client has a unique {@code orderToken} used for the public order submission URL.
 * The token is auto-generated on creation and can be rotated by the OWNER.
 */
@Getter
public class Client {

    private final Long id;
    private String name;
    private final String clientCode;  // e.g. PBS, AYI — used in order number prefix, immutable
    private Long clientTypeId;        // FK → client_types
    private BillingMode billingMode;
    private String contactPerson;
    private String phone;
    private String address;
    private UUID orderToken;
    private boolean active;
    private final Instant createdAt;

    public Client(
            Long id, String name, String clientCode, Long clientTypeId, BillingMode billingMode,
            String contactPerson, String phone, String address, UUID orderToken, boolean active, Instant createdAt
    ) {
        this.id = id;
        this.name = name;
        this.clientCode = clientCode;
        this.clientTypeId = clientTypeId;
        this.billingMode = billingMode;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.address = address;
        this.orderToken = orderToken;
        this.active = active;
        this.createdAt = createdAt;
    }

    public void update(String name, Long clientTypeId, BillingMode billingMode,
                       String contactPerson, String phone, String address) {
        this.name = name;
        this.clientTypeId = clientTypeId;
        this.billingMode = billingMode;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.address = address;
    }

    /**
     * Regenerates the order token, invalidating the old public link.
     */
    public void rotateToken() {
        this.orderToken = UUID.randomUUID();
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

}
