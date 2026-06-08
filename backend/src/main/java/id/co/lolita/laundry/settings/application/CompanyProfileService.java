package id.co.lolita.laundry.settings.application;

import id.co.lolita.laundry.settings.domain.CompanyProfile;
import id.co.lolita.laundry.settings.domain.port.in.CompanyProfileQuery;
import id.co.lolita.laundry.settings.domain.port.in.GetCompanyProfileUseCase;
import id.co.lolita.laundry.settings.domain.port.in.UpdateCompanyProfileUseCase;
import id.co.lolita.laundry.settings.domain.port.out.CompanyProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the singleton company profile and exposes it to other modules. Reads fall back to
 * {@link CompanyProfile#defaults()} until the OWNER saves a profile, so PDF rendering always has
 * company details available.
 */
@Service
@Transactional
@RequiredArgsConstructor
class CompanyProfileService implements GetCompanyProfileUseCase, UpdateCompanyProfileUseCase, CompanyProfileQuery {

    private final CompanyProfileRepository repository;

    @Override
    @Transactional(readOnly = true)
    public CompanyProfile get() {
        return repository.find().orElseGet(CompanyProfile::defaults);
    }

    @Override
    public CompanyProfile update(UpdateCompanyProfileCommand command) {
        var profile = repository.find().orElseGet(CompanyProfile::defaults);
        profile.update(command.companyName(), command.address(), command.phone(), command.bankBeneficiary(),
                command.bankName(), command.bankAccount(), command.bankHolder());
        return repository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyProfileView current() {
        var p = get();
        return new CompanyProfileView(p.getCompanyName(), p.getAddress(), p.getPhone(), p.getBankBeneficiary(),
                p.getBankName(), p.getBankAccount(), p.getBankHolder());
    }
}
