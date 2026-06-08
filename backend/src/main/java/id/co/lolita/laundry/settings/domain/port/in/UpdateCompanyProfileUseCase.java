package id.co.lolita.laundry.settings.domain.port.in;

import id.co.lolita.laundry.settings.domain.CompanyProfile;

public interface UpdateCompanyProfileUseCase {

    CompanyProfile update(UpdateCompanyProfileCommand command);

    record UpdateCompanyProfileCommand(String companyName, String address, String phone, String bankBeneficiary,
                                       String bankName, String bankAccount, String bankHolder) {
    }
}
