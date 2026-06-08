package id.co.lolita.laundry.settings.adapter.in.web.dto;

import id.co.lolita.laundry.settings.domain.CompanyProfile;

public record CompanyProfileResponse(String companyName, String address, String phone, String bankBeneficiary,
                                     String bankName, String bankAccount, String bankHolder) {

    public static CompanyProfileResponse from(CompanyProfile p) {
        return new CompanyProfileResponse(p.getCompanyName(), p.getAddress(), p.getPhone(), p.getBankBeneficiary(),
                p.getBankName(), p.getBankAccount(), p.getBankHolder());
    }
}