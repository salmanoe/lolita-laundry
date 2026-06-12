package id.co.lolita.laundry.client.domain.port.in;

import id.co.lolita.laundry.client.domain.Client;
import id.co.lolita.laundry.shared.Page;
import id.co.lolita.laundry.shared.PageQuery;

import java.util.List;

public interface GetClientUseCase {
    Page<Client> getClients(PageQuery query);

    /**
     * All active clients, name-sorted — backs the order-form hotel dropdown.
     */
    List<Client> getActiveClients();

    Client getClientById(Long id);
}
