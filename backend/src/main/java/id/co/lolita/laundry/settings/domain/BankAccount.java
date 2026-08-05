package id.co.lolita.laundry.settings.domain;

import lombok.Getter;

/**
 * One of the company's bank accounts, as printed in the "Please Transfer To" block of a monthly
 * billing INVOICE. The business runs more than one — a personal account and a company account —
 * and each client is billed to whichever one applies to them (large clients such as PBS use the
 * company account). A client with no explicit assignment falls back to the {@code default} account.
 *
 * <p>Accounts are soft-deactivated, never deleted: an account may still be named on frozen
 * invoices, and the client assignment is a plain FK.
 *
 * <p>Historical accuracy is handled by the {@code billing} module, which freezes these values onto
 * a monthly billing when it is ISSUED — reassigning a client afterwards never rewrites a document
 * the client already paid against.
 */
@Getter
public class BankAccount {

    private final Long id;
    private String label;
    private String beneficiary;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private boolean defaultAccount;
    private boolean active;
    private int sortOrder;

    public BankAccount(Long id, String label, String beneficiary, String bankName, String accountNumber,
                       String accountHolder, boolean defaultAccount, boolean active, int sortOrder) {
        this.id = id;
        this.label = label;
        this.beneficiary = beneficiary;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.defaultAccount = defaultAccount;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    /**
     * The built-in fallback, used only if the account table is somehow empty (no default, no
     * assignment). Mirrors the original hardcoded bank block so a billing PDF is never rendered
     * with a blank transfer section. Kept in sync with the {@code V17} seed, which copies these
     * same values out of the {@code V10} company profile.
     */
    public static BankAccount defaults() {
        return new BankAccount(null, "Rekening Pribadi", "Alban Valentino Ramatir", "Bank BCA",
                "4061792362", "Lolita Laundry", true, true, 0);
    }

    public void update(String label, String beneficiary, String bankName, String accountNumber,
                       String accountHolder, int sortOrder) {
        this.label = label;
        this.beneficiary = beneficiary;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.sortOrder = sortOrder;
    }

    /**
     * Promotes this account to the default. The caller is responsible for clearing the previous
     * default in the same transaction — the database enforces at most one via a partial unique
     * index, so leaving two set would fail the write.
     */
    public void markDefault() {
        this.defaultAccount = true;
        this.active = true;   // the fallback account must always be usable
    }

    public void clearDefault() {
        this.defaultAccount = false;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
