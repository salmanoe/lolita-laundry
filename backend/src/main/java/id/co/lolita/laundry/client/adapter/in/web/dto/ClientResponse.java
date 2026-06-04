package id.co.lolita.laundry.client.adapter.in.web.dto;

import id.co.lolita.laundry.client.domain.BillingMode;
import id.co.lolita.laundry.client.domain.Client;

import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
        Long id,
        String name,
        String clientCode,
        Long clientTypeId,
        BillingMode billingMode,
        String contactPerson,
        String phone,
        String address,
        UUID orderToken,
        boolean active,
        Instant createdAt
) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(), client.getName(), client.getClientCode(),
                client.getClientTypeId(), client.getBillingMode(),
                client.getContactPerson(), client.getPhone(), client.getAddress(),
                client.getOrderToken(), client.isActive(), client.getCreatedAt()
        );
    }
}