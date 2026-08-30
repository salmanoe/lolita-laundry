package id.co.lolita.laundry.client.domain;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * A client (hotel, restaurant, etc.) served by Lolita Laundry.
 *
 * <p>Each client has a unique {@code orderToken} used for the public order submission URL.
 * The token is auto-generated on creation and can be rotated by the OWNER.
 *
 * <p>{@code bankAccountId} picks which company bank account the client's monthly billing invoices
 * are payable to. Null — the normal case — means the default account; only clients that bill
 * somewhere else (PBS, to the company account) carry an explicit value.
 *
 * <p>{@code billingCycleDay} is the client's monthly billing cut-off. Null — the normal case —
 * means the plain calendar month (1st through the last day). A client with a contractual cut-off
 * (e.g. "invoice every 25th") carries that day, and its billing period then runs from the day
 * after the cut-off in the previous month through the cut-off itself.
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
    private Long bankAccountId;       // FK → bank_accounts; null = bill to the default account
    private Integer billingCycleDay;  // monthly cut-off day; null = plain calendar month
    private boolean active;
    private final Instant createdAt;

    public Client(
            Long id, String name, String clientCode, Long clientTypeId, BillingMode billingMode,
            String contactPerson, String phone, String address, UUID orderToken, Long bankAccountId,
            Integer billingCycleDay, boolean active, Instant createdAt
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
        this.bankAccountId = bankAccountId;
        this.billingCycleDay = billingCycleDay;
        this.active = active;
        this.createdAt = createdAt;
    }

    public void update(String name, Long clientTypeId, BillingMode billingMode,
                       String contactPerson, String phone, String address, Long bankAccountId,
                       Integer billingCycleDay) {
        this.name = name;
        this.clientTypeId = clientTypeId;
        this.billingMode = billingMode;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.address = address;
        this.bankAccountId = bankAccountId;
        this.billingCycleDay = billingCycleDay;
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
