package id.co.lolita.laundry.catalog.adapter.out.persistence;

import id.co.lolita.laundry.catalog.domain.ItemMaster;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "item_master")
@Getter
@Setter
@NoArgsConstructor
class ItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(nullable = false)
    private boolean active = true;

    static ItemJpaEntity fromDomain(ItemMaster item) {
        var entity = new ItemJpaEntity();
        entity.id = item.getId();
        entity.name = item.getName();
        entity.unitId = item.getUnitId();
        entity.active = item.isActive();
        return entity;
    }

    ItemMaster toDomain() {
        return new ItemMaster(id, name, unitId, active);
    }
}
