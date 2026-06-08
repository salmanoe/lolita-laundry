package id.co.lolita.laundry.settings.domain.port.out;

import id.co.lolita.laundry.settings.domain.CompanyProfile;

import java.util.Optional;

public interface CompanyProfileRepository {

    /**
     * The single company-profile row, if it has been persisted.
     */
    Optional<CompanyProfile> find();

    CompanyProfile save(CompanyProfile profile);
}
