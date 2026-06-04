package id.co.lolita.laundry.client.domain.port.in;

import id.co.lolita.laundry.client.domain.BillingMode;
import id.co.lolita.laundry.client.domain.Client;

public interface ManageClientUseCase {

    record CreateClientCommand(
            String name, String clientCode, Long clientTypeId, BillingMode billingMode,
            String contactPerson, String phone, String address
    ) {
    }

    record UpdateClientCommand(
            Long id, String name, Long clientTypeId, BillingMode billingMode,
            String contactPerson, String phone, String address
    ) {
    }

    Client createClient(CreateClientCommand command);

    Client updateClient(UpdateClientCommand command);

    Client rotateToken(Long clientId);
}
