package id.co.lolita.laundry.client.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Read-only pricing lookups other modules need. The {@code order} module uses
 * {@link #effectivePrice} to snapshot a line item's price at order creation, and
 * {@link #currentPrices} to render the public order form.
 *
 * <p>Exposed cross-module (named interface "api").
 */
public interface ClientPricingQuery {

    record PricePoint(Long itemId, BigDecimal pricePerUnit) {
    }

    /** A per-client item→department assignment (PER_DEPARTMENT clients only). */
    record ItemDepartment(Long itemId, Long departmentId) {
    }

    /** Effective unit price for an item for a client as of the given date (append-only history). */
    Optional<BigDecimal> effectivePrice(Long clientId, Long itemId, LocalDate asOf);

    /** Current effective unit price of every priced item for a client (as of today). */
    List<PricePoint> currentPrices(Long clientId);

    /** Every item→department assignment for a client (empty for COMBINED clients). */
    List<ItemDepartment> itemDepartments(Long clientId);

    /** The department an item is assigned to for a client, if any (PER_DEPARTMENT clients). */
    Optional<Long> departmentForItem(Long clientId, Long itemId);
}
