package id.co.lolita.laundry.settings.adapter.in.web;

import id.co.lolita.laundry.settings.adapter.in.web.dto.CompanyProfileResponse;
import id.co.lolita.laundry.settings.adapter.in.web.dto.UpdateCompanyProfileRequest;
import id.co.lolita.laundry.settings.domain.port.in.GetCompanyProfileUseCase;
import id.co.lolita.laundry.settings.domain.port.in.UpdateCompanyProfileUseCase;
import id.co.lolita.laundry.settings.domain.port.in.UpdateCompanyProfileUseCase.UpdateCompanyProfileCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OWNER-only company-profile editor. The profile (letterhead + bank details) appears on every
 * invoice and monthly-billing PDF, so it is read by STAFF too but only the OWNER may change it.
 */
@RestController
@RequestMapping("/api/company-profile")
@RequiredArgsConstructor
class CompanyProfileController {

    private final GetCompanyProfileUseCase getProfile;
    private final UpdateCompanyProfileUseCase updateProfile;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    CompanyProfileResponse get() {
        return CompanyProfileResponse.from(getProfile.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    CompanyProfileResponse update(@Valid @RequestBody UpdateCompanyProfileRequest request) {
        return CompanyProfileResponse.from(updateProfile.update(new UpdateCompanyProfileCommand(
                request.companyName(), request.address(), request.phone(), request.bankBeneficiary(),
                request.bankName(), request.bankAccount(), request.bankHolder())));
    }
}