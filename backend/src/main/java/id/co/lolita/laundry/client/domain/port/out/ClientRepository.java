package id.co.lolita.laundry.client.domain.port.out;

import id.co.lolita.laundry.client.domain.Client;
import id.co.lolita.laundry.shared.Page;
import id.co.lolita.laundry.shared.PageQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository {
    Page<Client> findAll(PageQuery query);

    List<Client> findAllActive();

    Optional<Client> findById(Long id);

    Optional<Client> findByOrderToken(UUID token);

    boolean existsByClientCode(String clientCode);

    Client save(Client client);
}
