package id.co.lolita.laundry.client.domain.port.in;

import id.co.lolita.laundry.client.domain.ClientType;

import java.util.List;

public interface ClientTypeUseCase {
    List<ClientType> list();

    List<ClientType> listActive();

    ClientType create(CreateLookupCommand command);

    ClientType update(UpdateLookupCommand command);
}