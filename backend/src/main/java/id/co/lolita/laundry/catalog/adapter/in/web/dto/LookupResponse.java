package id.co.lolita.laundry.catalog.adapter.in.web.dto;

import id.co.lolita.laundry.catalog.domain.ItemCategory;
import id.co.lolita.laundry.catalog.domain.ItemUnit;

/**
 * Response shape shared by item units and item categories (same fields).
 */
public record LookupResponse(Long id, String code, String displayName, int sortOrder, boolean active) {

    public static LookupResponse from(ItemUnit u) {
        return new LookupResponse(u.getId(), u.getCode(), u.getDisplayName(), u.getSortOrder(), u.isActive());
    }

    public static LookupResponse from(ItemCategory c) {
        return new LookupResponse(c.getId(), c.getCode(), c.getDisplayName(), c.getSortOrder(), c.isActive());
    }
}