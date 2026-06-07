package id.co.lolita.laundry.catalog.domain;

import lombok.Getter;

/**
 * A laundry item in the master catalogue.
 * Items are shared across all clients; per-client pricing is in ClientPriceList (client module)
 * and, for PER_DEPARTMENT clients, the per-client item→department mapping lives there too.
 */
@Getter
public class ItemMaster {

    private final Long id;
    private String name;
    private Long unitId;      // FK → item_units
    private boolean active;

    public ItemMaster(Long id, String name, Long unitId, boolean active) {
        this.id = id;
        this.name = name;
        this.unitId = unitId;
        this.active = active;
    }

    public void update(String name, Long unitId) {
        this.name = name;
        this.unitId = unitId;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
