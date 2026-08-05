package id.co.lolita.laundry.billing.domain.port.out;

/**
 * Billing's view of the company settings it prints on documents: the letterhead, shared by every
 * document, and the bank account, which is chosen per client.
 *
 * <p>The adapter delegates to the settings module's {@code CompanyProfileQuery} and
 * {@code BankAccountQuery} (named interface {@code settings::api}). Neither call is ever empty —
 * the settings module falls back to built-in defaults.
 */
public interface CompanyProfileGateway {

    record CompanyInfo(String companyName, String address, String phone) {
    }

    record BankInfo(String beneficiary, String bankName, String accountNumber, String accountHolder) {
    }

    CompanyInfo current();

    /**
     * The bank account a client's invoice is payable to. Never null: an unassigned client
     * ({@code bankAccountId == null}), or one pointing at a removed or deactivated account,
     * resolves to the default account.
     *
     * @param bankAccountId the client's assignment, or {@code null} for "use the default"
     */
    BankInfo bankAccount(Long bankAccountId);
}
