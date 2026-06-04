package id.co.lolita.laundry.catalog.domain.port.in;

import id.co.lolita.laundry.catalog.domain.ItemCategory;

import java.util.List;

public interface ItemCategoryUseCase {
    List<ItemCategory> list();

    List<ItemCategory> listActive();

    ItemCategory create(CreateLookupCommand command);

    ItemCategory update(UpdateLookupCommand command);
}
