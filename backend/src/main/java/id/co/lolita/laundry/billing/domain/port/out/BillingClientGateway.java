package id.co.lolita.laundry.billing.domain.port.out;

import id.co.lolita.laundry.billing.domain.BillingCycle;

import java.util.Optional;

/**
 * Billing's view of client directory data needed for invoice/billing headers and numbering.
 * The adapter delegates to the client module's {@code ClientDirectoryQuery}
 * (named interface {@code client::api}).
 */
public interface BillingClientGateway {

    /**
     * @param bankAccountId   which company bank account this client's invoices are payable to;
     *                        null means the default account
     * @param billingCycleDay the client's monthly billing cut-off day; null means the plain
     *                        calendar month
     */
    record ClientInfo(Long id, String name, String clientCode, boolean perDepartment, Long bankAccountId,
                      Integer billingCycleDay) {

        /** The client's billing cycle — {@link BillingCycle#CALENDAR} unless a cut-off is set. */
        public BillingCycle cycle() {
            return BillingCycle.of(billingCycleDay);
        }
    }

    Optional<ClientInfo> findById(Long clientId);
}