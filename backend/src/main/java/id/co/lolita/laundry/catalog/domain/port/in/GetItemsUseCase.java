package id.co.lolita.laundry.catalog.domain.port.in;

import id.co.lolita.laundry.catalog.domain.ItemMaster;
import id.co.lolita.laundry.shared.Page;
import id.co.lolita.laundry.shared.PageQuery;

import java.util.List;

public interface GetItemsUseCase {
    /**
     * Returns a page of items (active and inactive). OWNER/STAFF use this for management.
     */
    Page<ItemMaster> getItems(PageQuery query);

    /**
     * Returns all active items (unpaged). Used for selection dropdowns and the public order form.
     */
    List<ItemMaster> getActiveItems();
}
