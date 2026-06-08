package id.co.lolita.laundry.billing.adapter.out.gateway;

import id.co.lolita.laundry.billing.domain.port.out.CompanyProfileGateway;
import id.co.lolita.laundry.settings.domain.port.in.CompanyProfileQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bridges billing's {@link CompanyProfileGateway} to the settings module's
 * {@link CompanyProfileQuery} (named interface {@code settings::api}).
 */
@Component
@RequiredArgsConstructor
class CompanyProfileGatewayAdapter implements CompanyProfileGateway {

    private final CompanyProfileQuery companyProfile;

    @Override
    public CompanyInfo current() {
        var p = companyProfile.current();
        return new CompanyInfo(p.companyName(), p.address(), p.phone(), p.bankBeneficiary(),
                p.bankName(), p.bankAccount(), p.bankHolder());
    }
}