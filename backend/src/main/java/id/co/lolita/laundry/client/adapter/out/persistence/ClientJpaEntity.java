package id.co.lolita.laundry.client.adapter.out.persistence;

import id.co.lolita.laundry.client.domain.BillingMode;
import id.co.lolita.laundry.client.domain.Client;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
class ClientJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "client_code", nullable = false, unique = true, length = 10)
    private String clientCode;

    @Column(name = "client_type_id", nullable = false)
    private Long clientTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", nullable = false, length = 20)
    private BillingMode billingMode;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "order_token", nullable = false, unique = true)
    private UUID orderToken;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    static ClientJpaEntity fromDomain(Client client) {
        var e = new ClientJpaEntity();
        e.id = client.getId();
        e.name = client.getName();
        e.clientCode = client.getClientCode();
        e.clientTypeId = client.getClientTypeId();
        e.billingMode = client.getBillingMode();
        e.contactPerson = client.getContactPerson();
        e.phone = client.getPhone();
        e.address = client.getAddress();
        e.orderToken = client.getOrderToken();
        e.active = client.isActive();
        e.createdAt = client.getCreatedAt();
        return e;
    }

    Client toDomain() {
        return new Client(id, name, clientCode, clientTypeId, billingMode,
                contactPerson, phone, address, orderToken, active, createdAt);
    }
}
