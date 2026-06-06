package id.co.lolita.laundry.order.adapter.out.gateway;

import id.co.lolita.laundry.client.domain.port.in.ClientDirectoryQuery;
import id.co.lolita.laundry.order.domain.port.out.ClientGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Bridges the order module to the client module's exposed directory API, mapping the
 * client module's view into the order module's own {@link ClientSnapshot}.
 */
@Component
@RequiredArgsConstructor
class ClientGatewayAdapter implements ClientGateway {

    private final ClientDirectoryQuery directory;

    @Override
    public Optional<ClientSnapshot> findByToken(UUID token) {
        return directory.findByToken(token).map(ClientGatewayAdapter::toSnapshot);
    }

    @Override
    public Optional<ClientSnapshot> findById(Long clientId) {
        return directory.findById(clientId).map(ClientGatewayAdapter::toSnapshot);
    }

    private static ClientSnapshot toSnapshot(ClientDirectoryQuery.ClientView v) {
        return new ClientSnapshot(v.id(), v.name(), v.clientCode(), v.active(), v.perDepartment());
    }
}