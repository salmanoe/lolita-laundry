package id.co.lolita.laundry.client.domain.port.in;

import id.co.lolita.laundry.client.domain.BillingMode;
import id.co.lolita.laundry.client.domain.Client;

public interface ManageClientUseCase {

    /**
     * {@code bankAccountId} is optional — null means the client bills to the default bank account.
     */
    record CreateClientCommand(
            String name, String clientCode, Long clientTypeId, BillingMode billingMode,
            String contactPerson, String phone, String address, Long bankAccountId
    ) {
    }

    record UpdateClientCommand(
            Long id, String name, Long clientTypeId, BillingMode billingMode,
            String contactPerson, String phone, String address, Long bankAccountId
    ) {
    }

    Client createClient(CreateClientCommand command);

    Client updateClient(UpdateClientCommand command);

    Client rotateToken(Long clientId);
}
