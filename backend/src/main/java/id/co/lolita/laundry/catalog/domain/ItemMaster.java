package id.co.lolita.laundry.catalog.domain;

import lombok.Getter;

/**
 * A laundry item in the master catalogue.
 * Items are shared across all clients; per-client pricing is in ClientPriceList (client module).
 */
@Getter
public class ItemMaster {

    private final Long id;
    private String name;
    private Long unitId;      // FK → item_units
    private Long categoryId;  // FK → item_categories
    private boolean active;

    public ItemMaster(Long id, String name, Long unitId, Long categoryId, boolean active) {
        this.id = id;
        this.name = name;
        this.unitId = unitId;
        this.categoryId = categoryId;
        this.active = active;
    }

    public void update(String name, Long unitId, Long categoryId) {
        this.name = name;
        this.unitId = unitId;
        this.categoryId = categoryId;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
