package id.co.lolita.laundry.client.adapter.in.web.dto;

import id.co.lolita.laundry.client.domain.Client;

/**
 * Lightweight client option for the in-house order-form hotel dropdown — id, code, name only.
 * Readable by DAILY_STAFF (who otherwise have no access to the full client list).
 */
public record ClientOptionResponse(Long id, String clientCode, String name) {
    public static ClientOptionResponse from(Client client) {
        return new ClientOptionResponse(client.getId(), client.getClientCode(), client.getName());
    }
}
