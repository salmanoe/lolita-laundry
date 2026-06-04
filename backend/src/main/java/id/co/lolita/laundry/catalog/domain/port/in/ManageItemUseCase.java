package id.co.lolita.laundry.catalog.domain.port.in;

import id.co.lolita.laundry.catalog.domain.ItemMaster;

public interface ManageItemUseCase {

    record CreateItemCommand(String name, Long unitId, Long categoryId) {
    }

    record UpdateItemCommand(Long id, String name, Long unitId, Long categoryId, boolean active) {
    }

    ItemMaster createItem(CreateItemCommand command);

    ItemMaster updateItem(UpdateItemCommand command);
}