package id.co.lolita.laundry.order.domain.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Order module's view of the item catalogue.
 */
public interface CatalogGateway {

    record CatalogItem(Long id, String name, Long unitId, String unitName,
                       Long categoryId, String categoryName) {
    }

    List<CatalogItem> activeItems();

    Optional<CatalogItem> findActiveById(Long itemId);
}