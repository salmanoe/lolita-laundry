package id.co.lolita.laundry.billing.domain.port.out;

import java.util.Optional;

/**
 * Billing's view of client directory data needed for invoice/billing headers and numbering.
 * The adapter delegates to the client module's {@code ClientDirectoryQuery}
 * (named interface {@code client::api}).
 */
public interface BillingClientGateway {

    record ClientInfo(Long id, String name, String clientCode, boolean perDepartment) {
    }

    Optional<ClientInfo> findById(Long clientId);
}