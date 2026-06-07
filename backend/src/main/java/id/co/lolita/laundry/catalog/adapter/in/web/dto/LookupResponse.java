package id.co.lolita.laundry.catalog.adapter.in.web.dto;

import id.co.lolita.laundry.catalog.domain.ItemUnit;

/**
 * Response shape for item-unit lookups.
 */
public record LookupResponse(Long id, String code, String displayName, int sortOrder, boolean active) {

    public static LookupResponse from(ItemUnit u) {
        return new LookupResponse(u.getId(), u.getCode(), u.getDisplayName(), u.getSortOrder(), u.isActive());
    }
}