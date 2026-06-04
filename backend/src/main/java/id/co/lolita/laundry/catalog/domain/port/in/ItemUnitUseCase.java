package id.co.lolita.laundry.catalog.domain.port.in;

import id.co.lolita.laundry.catalog.domain.ItemUnit;

import java.util.List;

public interface ItemUnitUseCase {
    List<ItemUnit> list();

    List<ItemUnit> listActive();

    ItemUnit create(CreateLookupCommand command);

    ItemUnit update(UpdateLookupCommand command);
}
