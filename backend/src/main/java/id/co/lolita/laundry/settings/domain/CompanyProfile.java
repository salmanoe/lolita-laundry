package id.co.lolita.laundry.settings.domain;

import lombok.Getter;

/**
 * The company's own letterhead, as it appears on every invoice and monthly billing PDF. A
 * singleton — there is exactly one row (id {@code 1}) — editable by the SUPER_ADMIN so the address
 * or phone can change mid-business without a code change.
 *
 * <p>Bank-transfer details used to live here too. They moved to {@link BankAccount} in {@code V17}
 * when the business started running more than one account, because the account to print is chosen
 * per client rather than being a single company-wide value.
 *
 * <p>Historical accuracy is handled by the {@code billing} module: a billing freezes a snapshot
 * of the letterhead (and its bank block) when it is ISSUED, and an order invoice freezes it at
 * delivery, so changing the profile here never silently rewrites a document a client already paid
 * against.
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

    public CompanyProfile(Long id, String companyName, String address, String phone) {
        this.id = id;
        this.companyName = companyName;
        this.address = address;
        this.phone = phone;
    }

    /**
     * The built-in fallback, used before the SUPER_ADMIN has saved a profile (and if the seeded row
     * is ever missing). Mirrors the original hardcoded letterhead so PDFs always render with sane
     * company details. Kept in sync with the {@code V10} seed.
     */
    public static CompanyProfile defaults() {
        return new CompanyProfile(SINGLETON_ID, "Lolita Laundry", "Jl. Sukaraja No. 318 Bandung", "082318359775");
    }

    public void update(String companyName, String address, String phone) {
        this.companyName = companyName;
        this.address = address;
        this.phone = phone;
    }
}
