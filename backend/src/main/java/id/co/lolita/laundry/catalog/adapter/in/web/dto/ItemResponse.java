package id.co.lolita.laundry.catalog.adapter.in.web.dto;

import id.co.lolita.laundry.catalog.domain.ItemMaster;

public record ItemResponse(
        Long id,
        String name,
        Long unitId,
        Long categoryId,
        boolean active
) {
    public static ItemResponse from(ItemMaster item) {
        return new ItemResponse(item.getId(), item.getName(), item.getUnitId(), item.getCategoryId(), item.isActive());
    }
}