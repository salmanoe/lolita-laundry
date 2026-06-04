package id.co.lolita.laundry.catalog.domain.port.out;

import id.co.lolita.laundry.catalog.domain.ItemMaster;
import id.co.lolita.laundry.shared.Page;
import id.co.lolita.laundry.shared.PageQuery;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {
    Page<ItemMaster> findAll(PageQuery query);

    List<ItemMaster> findAllActive();

    Optional<ItemMaster> findById(Long id);

    boolean existsByName(String name);

    ItemMaster save(ItemMaster item);
}
