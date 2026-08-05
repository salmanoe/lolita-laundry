package id.co.lolita.laundry.client.domain.port.out;

import java.util.Optional;

/**
 * Client's view of the company's bank accounts, used to validate a client's assignment before it
 * reaches the database. Without it a bad id would surface as a raw FK violation rather than a
 * readable 400.
 *
 * <p>The adapter delegates to the settings module's {@code BankAccountQuery} (named interface
 * {@code settings::api}).
 */
public interface BankAccountLookupGateway {

    record BankAccountRef(Long id, String label, boolean active) {
    }

    Optional<BankAccountRef> findById(Long bankAccountId);
}
