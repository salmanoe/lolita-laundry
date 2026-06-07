package id.co.lolita.laundry.catalog.domain.port.in;

import java.util.List;
import java.util.Optional;

/**
 * Read-only catalogue lookups other modules need. The {@code order} module uses this to
 * render the public order form and to validate item references at order creation.
 *
 * <p>Exposed cross-module (named interface "api"). Returns self-contained records — no
 * catalog domain types leak across the boundary.
 */
public interface CatalogQuery {

    record CatalogItemSnapshot(Long id, String name, Long unitId, String unitName) {
    }

    List<CatalogItemSnapshot> activeItems();

    Optional<CatalogItemSnapshot> findActiveById(Long itemId);
}