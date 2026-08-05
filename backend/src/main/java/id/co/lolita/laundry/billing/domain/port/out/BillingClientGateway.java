package id.co.lolita.laundry.billing.domain.port.out;

import java.util.Optional;

/**
 * Billing's view of client directory data needed for invoice/billing headers and numbering.
 * The adapter delegates to the client module's {@code ClientDirectoryQuery}
 * (named interface {@code client::api}).
 */
public interface BillingClientGateway {

    /**
     * @param bankAccountId which company bank account this client's invoices are payable to;
     *                      null means the default account
     */
    record ClientInfo(Long id, String name, String clientCode, boolean perDepartment, Long bankAccountId) {
    }

    Optional<ClientInfo> findById(Long clientId);
}