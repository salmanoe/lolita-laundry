package id.co.lolita.laundry.catalog.adapter.out.persistence;

import id.co.lolita.laundry.catalog.domain.ItemUnit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "item_units")
@Getter
@Setter
@NoArgsConstructor
class ItemUnitJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    static ItemUnitJpaEntity fromDomain(ItemUnit u) {
        var e = new ItemUnitJpaEntity();
        e.id = u.getId();
        e.code = u.getCode();
        e.displayName = u.getDisplayName();
        e.sortOrder = u.getSortOrder();
        e.active = u.isActive();
        return e;
    }

    ItemUnit toDomain() {
        return new ItemUnit(id, code, displayName, sortOrder, active);
    }
}