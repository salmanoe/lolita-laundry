package id.co.lolita.laundry.client.adapter.in.web.dto;

import id.co.lolita.laundry.client.domain.ClientType;

/**
 * Response shape for client-type reference data.
 */
public record LookupResponse(Long id, String code, String displayName, int sortOrder, boolean active) {

    public static LookupResponse from(ClientType t) {
        return new LookupResponse(t.getId(), t.getCode(), t.getDisplayName(), t.getSortOrder(), t.isActive());
    }
}