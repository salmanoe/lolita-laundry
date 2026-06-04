package id.co.lolita.laundry.client.domain.port.out;

import id.co.lolita.laundry.client.domain.ClientType;

import java.util.List;
import java.util.Optional;

public interface ClientTypeRepository {
    List<ClientType> findAll();

    List<ClientType> findAllActive();

    Optional<ClientType> findById(Long id);

    boolean existsByCode(String code);

    ClientType save(ClientType clientType);
}