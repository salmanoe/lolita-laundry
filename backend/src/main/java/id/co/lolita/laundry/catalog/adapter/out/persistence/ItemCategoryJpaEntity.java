package id.co.lolita.laundry.catalog.adapter.out.persistence;

import id.co.lolita.laundry.catalog.domain.ItemCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "item_categories")
@Getter
@Setter
@NoArgsConstructor
class ItemCategoryJpaEntity {

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

    static ItemCategoryJpaEntity fromDomain(ItemCategory c) {
        var e = new ItemCategoryJpaEntity();
        e.id = c.getId();
        e.code = c.getCode();
        e.displayName = c.getDisplayName();
        e.sortOrder = c.getSortOrder();
        e.active = c.isActive();
        return e;
    }

    ItemCategory toDomain() {
        return new ItemCategory(id, code, displayName, sortOrder, active);
    }
}
