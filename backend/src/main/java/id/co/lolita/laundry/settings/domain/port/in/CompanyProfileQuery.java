package id.co.lolita.laundry.settings.domain.port.in;

/**
 * Read-only access to the company profile for other modules (cross-module named interface
 * "api"). Returns a self-contained record — no settings domain types leak across the boundary.
 * Never empty: callers get the saved profile, or the built-in defaults if none has been saved.
 */
public interface CompanyProfileQuery {

    record CompanyProfileView(String companyName, String address, String phone, String bankBeneficiary,
                              String bankName, String bankAccount, String bankHolder) {
    }

    CompanyProfileView current();
}
