package id.co.lolita.laundry.client.domain;

import lombok.Getter;

/**
 * The kind of business a client is (Hotel, Restaurant, Spa, …). Managed as reference data
 * so the OWNER can add types without a code change. {@code code} is the stable identifier
 * (immutable); {@code displayName} is the editable label shown in the UI.
 *
 * <p>Note: unlike {@link BillingMode} (which drives billing logic and stays a fixed enum),
 * client type is purely descriptive, so it is safe to make dynamic.
 */
@Getter
public class ClientType {

    private final Long id;
    private final String code;
    private String displayName;
    private int sortOrder;
    private boolean active;

    public ClientType(Long id, String code, String displayName, int sortOrder, boolean active) {
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