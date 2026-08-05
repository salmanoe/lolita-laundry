package id.co.lolita.laundry.settings.domain.port.in;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to the company's bank accounts for other modules (cross-module named interface
 * "api"). Returns self-contained records — no settings domain types leak across the boundary.
 *
 * <p>{@link #resolve(Long)} is the single home of the fallback chain, so consumers (billing when
 * rendering an invoice, client when labelling an assignment) never reimplement it.
 */
public interface BankAccountQuery {

    record BankAccountView(Long id, String label, String beneficiary, String bankName,
                           String accountNumber, String accountHolder, boolean active) {
    }

    Optional<BankAccountView> findById(Long id);

    /**
     * The account a document should print, given a client's assignment:
     * the assigned account if it exists and is still active, otherwise the default account,
     * otherwise the built-in fallback. Never empty — a billing PDF must never render a blank
     * transfer block.
     *
     * @param bankAccountId the client's assignment, or {@code null} for "use the default"
     */
    BankAccountView resolve(Long bankAccountId);

    List<BankAccountView> activeAccounts();
}
