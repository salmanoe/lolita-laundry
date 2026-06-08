package id.co.lolita.laundry.settings.domain.port.in;

import id.co.lolita.laundry.settings.domain.CompanyProfile;

public interface GetCompanyProfileUseCase {

    /**
     * The saved company profile, or the built-in defaults if none has been saved yet.
     */
    CompanyProfile get();
}
