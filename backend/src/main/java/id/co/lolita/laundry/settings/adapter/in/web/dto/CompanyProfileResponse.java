package id.co.lolita.laundry.settings.adapter.in.web.dto;

import id.co.lolita.laundry.settings.domain.CompanyProfile;

public record CompanyProfileResponse(String companyName, String address, String phone) {

    public static CompanyProfileResponse from(CompanyProfile p) {
        return new CompanyProfileResponse(p.getCompanyName(), p.getAddress(), p.getPhone());
    }
}