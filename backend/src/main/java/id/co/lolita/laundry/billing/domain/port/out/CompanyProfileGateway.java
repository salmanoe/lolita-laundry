package id.co.lolita.laundry.billing.domain.port.out;

/**
 * Billing's view of the company's own letterhead and bank details, printed on invoice and
 * monthly-billing PDFs. The adapter delegates to the settings module's
 * {@code CompanyProfileQuery} (named interface {@code settings::api}). Never empty — the settings
 * module falls back to built-in defaults until the OWNER saves a profile.
 */
public interface CompanyProfileGateway {

    record CompanyInfo(String companyName, String address, String phone, String bankBeneficiary,
                       String bankName, String bankAccount, String bankHolder) {
    }

    CompanyInfo current();
}