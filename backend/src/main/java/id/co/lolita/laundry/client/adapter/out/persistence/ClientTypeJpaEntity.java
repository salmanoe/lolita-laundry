package id.co.lolita.laundry.client.adapter.out.persistence;

import id.co.lolita.laundry.client.domain.ClientType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "client_types")
@Getter
@Setter
@NoArgsConstructor
class ClientTypeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    static ClientTypeJpaEntity fromDomain(ClientType t) {
        var e = new ClientTypeJpaEntity();
        e.id = t.getId();
        e.code = t.getCode();
        e.displayName = t.getDisplayName();
        e.sortOrder = t.getSortOrder();
        e.active = t.isActive();
        return e;
    }

    ClientType toDomain() {
        return new ClientType(id, code, displayName, sortOrder, active);
    }
}