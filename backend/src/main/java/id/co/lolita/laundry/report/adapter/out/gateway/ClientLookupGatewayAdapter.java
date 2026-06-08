package id.co.lolita.laundry.report.adapter.out.gateway;

import id.co.lolita.laundry.client.domain.port.in.ClientDirectoryQuery;
import id.co.lolita.laundry.report.domain.port.out.ClientLookupGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Bridges report's {@link ClientLookupGateway} to the client module's
 * {@link ClientDirectoryQuery} (named interface {@code client::api}).
 */
@Component
@RequiredArgsConstructor
class ClientLookupGatewayAdapter implements ClientLookupGateway {

    private final ClientDirectoryQuery clients;

    @Override
    public Optional<ClientInfo> findById(Long clientId) {
        return clients.findById(clientId)
                .map(c -> new ClientInfo(c.id(), c.name(), c.clientCode()));
    }
}