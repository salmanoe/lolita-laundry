package id.co.lolita.laundry.catalog.domain.port.out;

import id.co.lolita.laundry.catalog.domain.ItemUnit;

import java.util.List;
import java.util.Optional;

public interface ItemUnitRepository {
    List<ItemUnit> findAll();

    List<ItemUnit> findAllActive();

    Optional<ItemUnit> findById(Long id);

    boolean existsByCode(String code);

    ItemUnit save(ItemUnit unit);
}
