package id.co.lolita.laundry.catalog.domain;

import lombok.Getter;

/**
 * A grouping for catalogue items (e.g. Bed Linen, Bath, F&B). Managed as reference data
 * so the OWNER can add categories without a code change. {@code code} is the stable
 * identifier (immutable); {@code displayName} is the editable label shown in the UI.
 */
@Getter
public class ItemCategory {

    private final Long id;
    private final String code;
    private String displayName;
    private int sortOrder;
    private boolean active;

    public ItemCategory(Long id, String code, String displayName, int sortOrder, boolean active) {
        this.id = id;
        this.code = code;
        this.displayName = displayName;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    public void update(String displayName, int sortOrder) {
        this.displayName = displayName;
        this.sortOrder = sortOrder;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
