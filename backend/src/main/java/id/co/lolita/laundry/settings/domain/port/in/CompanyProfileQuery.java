package id.co.lolita.laundry.settings.domain.port.in;

/**
 * Read-only access to the company letterhead for other modules (cross-module named interface
 * "api"). Returns a self-contained record — no settings domain types leak across the boundary.
 * Never empty: callers get the saved profile, or the built-in defaults if none has been saved.
 *
 * <p>Bank-transfer details are <em>not</em> here — they are per-client and come from
 * {@link BankAccountQuery} instead.
 */
public interface CompanyProfileQuery {

    record CompanyProfileView(String companyName, String address, String phone) {
    }

    CompanyProfileView current();
}
