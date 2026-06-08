package id.co.lolita.laundry.settings.domain;

import lombok.Getter;

/**
 * The company's own letterhead and bank-transfer details, as they appear on every invoice and
 * monthly billing PDF. A singleton — there is exactly one row (id {@code 1}) — editable by the
 * OWNER so the address, phone, or bank account can change mid-business without a code change.
 *
 * <p>Historical accuracy is handled by the {@code billing} module: a billing freezes a snapshot
 * of these fields when it is ISSUED, and an order invoice freezes them at creation, so changing
 * the profile here never silently rewrites a document a client already paid against.
 */
@Getter
public class CompanyProfile {

    /**
     * The single-row key. The profile is a singleton; there is never more than one.
     */
    public static final Long SINGLETON_ID = 1L;

    private final Long id;
    private String companyName;
    private String address;
    private String phone;
    private String bankBeneficiary;
    private String bankName;
    private String bankAccount;
    private String bankHolder;

    public CompanyProfile(Long id, String companyName, String address, String phone, String bankBeneficiary,
                          String bankName, String bankAccount, String bankHolder) {
        this.id = id;
        this.companyName = companyName;
        this.address = address;
        this.phone = phone;
        this.bankBeneficiary = bankBeneficiary;
        this.bankName = bankName;
        this.bankAccount = bankAccount;
        this.bankHolder = bankHolder;
    }

    /**
     * The built-in fallback, used before the OWNER has saved a profile (and if the seeded row is
     * ever missing). Mirrors the original hardcoded letterhead so PDFs always render with sane
     * company details. Kept in sync with the {@code V10} seed.
     */
    public static CompanyProfile defaults() {
        return new CompanyProfile(SINGLETON_ID, "Lolita Laundry", "Jl. Sukaraja No. 318 Bandung",
                "082318359775", "Alban Valentino Ramatir", "Bank BCA", "4061792362", "Lolita Laundry");
    }

    public void update(String companyName, String address, String phone, String bankBeneficiary,
                       String bankName, String bankAccount, String bankHolder) {
        this.companyName = companyName;
        this.address = address;
        this.phone = phone;
        this.bankBeneficiary = bankBeneficiary;
        this.bankName = bankName;
        this.bankAccount = bankAccount;
        this.bankHolder = bankHolder;
    }
}