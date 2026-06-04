package id.co.lolita.laundry.catalog.domain.port.out;

import id.co.lolita.laundry.catalog.domain.ItemCategory;

import java.util.List;
import java.util.Optional;

public interface ItemCategoryRepository {
    List<ItemCategory> findAll();

    List<ItemCategory> findAllActive();

    Optional<ItemCategory> findById(Long id);

    boolean existsByCode(String code);

    ItemCategory save(ItemCategory category);
}
