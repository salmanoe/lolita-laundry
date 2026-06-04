package id.co.lolita.laundry.client.adapter.out.persistence;

import id.co.lolita.laundry.client.domain.ClientPriceList;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "client_price_lists")
@Getter
@Setter
@NoArgsConstructor
class ClientPriceListJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "price_per_unit", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerUnit;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    static ClientPriceListJpaEntity fromDomain(ClientPriceList entry) {
        var e = new ClientPriceListJpaEntity();
        e.id = entry.id();
        e.clientId = entry.clientId();
        e.itemId = entry.itemId();
        e.pricePerUnit = entry.pricePerUnit();
        e.effectiveDate = entry.effectiveDate();
        e.createdAt = entry.createdAt();
        return e;
    }

    ClientPriceList toDomain() {
        return new ClientPriceList(id, clientId, itemId, pricePerUnit, effectiveDate, createdAt);
    }
}
