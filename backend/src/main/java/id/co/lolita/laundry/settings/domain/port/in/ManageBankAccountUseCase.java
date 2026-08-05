package id.co.lolita.laundry.settings.domain.port.in;

import id.co.lolita.laundry.settings.domain.BankAccount;

import java.util.List;

/**
 * SUPER_ADMIN administration of the company's bank accounts. Accounts are never deleted — an
 * account may be named on invoices already sent, and clients reference it by FK — so removal is a
 * deactivation.
 */
public interface ManageBankAccountUseCase {

    record CreateBankAccountCommand(String label, String beneficiary, String bankName, String accountNumber,
                                    String accountHolder, int sortOrder) {
    }

    record UpdateBankAccountCommand(Long id, String label, String beneficiary, String bankName, String accountNumber,
                                    String accountHolder, int sortOrder) {
    }

    List<BankAccount> list();

    BankAccount create(CreateBankAccountCommand command);

    BankAccount update(UpdateBankAccountCommand command);

    /**
     * Promotes an account to the default, clearing the previous one. The default is the account
     * used by every client without an explicit assignment.
     */
    BankAccount setDefault(Long id);

    /**
     * Activates or deactivates an account. Deactivating the default is rejected — it is the
     * fallback every unassigned client relies on.
     */
    BankAccount setActive(Long id, boolean active);
}
