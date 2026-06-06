package id.co.lolita.laundry.billing.adapter.out.gateway;

import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway;
import id.co.lolita.laundry.client.domain.port.in.ClientDirectoryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Bridges billing's {@link BillingClientGateway} to the client module's
 * {@link ClientDirectoryQuery} (named interface {@code client::api}).
 */
@Component
@RequiredArgsConstructor
class BillingClientGatewayAdapter implements BillingClientGateway {

    private final ClientDirectoryQuery clients;

    @Override
    public Optional<ClientInfo> findById(Long clientId) {
        return clients.findById(clientId)
                .map(c -> new ClientInfo(c.id(), c.name(), c.clientCode(), c.perDepartment()));
    }
}