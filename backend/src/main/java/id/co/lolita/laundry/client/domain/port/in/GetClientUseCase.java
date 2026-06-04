package id.co.lolita.laundry.client.domain.port.in;

import id.co.lolita.laundry.client.domain.Client;
import id.co.lolita.laundry.shared.Page;
import id.co.lolita.laundry.shared.PageQuery;

import java.util.UUID;

public interface GetClientUseCase {
    Page<Client> getClients(PageQuery query);

    Client getClientById(Long id);

    Client getClientByToken(UUID token);
}
