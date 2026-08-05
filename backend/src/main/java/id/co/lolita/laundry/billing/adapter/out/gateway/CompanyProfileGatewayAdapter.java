package id.co.lolita.laundry.billing.adapter.out.gateway;

import id.co.lolita.laundry.billing.domain.port.out.CompanyProfileGateway;
import id.co.lolita.laundry.settings.domain.port.in.BankAccountQuery;
import id.co.lolita.laundry.settings.domain.port.in.CompanyProfileQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bridges billing's {@link CompanyProfileGateway} to the settings module's
 * {@link CompanyProfileQuery} and {@link BankAccountQuery} (named interface {@code settings::api}).
 */
@Component
@RequiredArgsConstructor
class CompanyProfileGatewayAdapter implements CompanyProfileGateway {

    private final CompanyProfileQuery companyProfile;
    private final BankAccountQuery bankAccounts;

    @Override
    public CompanyInfo current() {
        var p = companyProfile.current();
        return new CompanyInfo(p.companyName(), p.address(), p.phone());
    }

    @Override
    public BankInfo bankAccount(Long bankAccountId) {
        // resolve() owns the assigned -> default -> built-in fallback chain.
        var a = bankAccounts.resolve(bankAccountId);
        return new BankInfo(a.beneficiary(), a.bankName(), a.accountNumber(), a.accountHolder());
    }
}
