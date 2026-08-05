package id.co.lolita.laundry.settings.domain.port.out;

import id.co.lolita.laundry.settings.domain.BankAccount;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository {

    /**
     * Every account, active or not, ordered by sort order then label.
     */
    List<BankAccount> findAll();

    /**
     * Only the accounts that can still be assigned to a client.
     */
    List<BankAccount> findActive();

    Optional<BankAccount> findById(Long id);

    /**
     * The account flagged as default — the one used by any client with no explicit assignment.
     * Empty only if the table has never been seeded.
     */
    Optional<BankAccount> findDefault();

    BankAccount save(BankAccount account);
}
