package id.co.lolita.laundry.client.adapter.out.gateway;

import id.co.lolita.laundry.client.domain.port.out.BankAccountLookupGateway;
import id.co.lolita.laundry.settings.domain.port.in.BankAccountQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Bridges client's {@link BankAccountLookupGateway} to the settings module's
 * {@link BankAccountQuery} (named interface {@code settings::api}).
 */
@Component
@RequiredArgsConstructor
class BankAccountLookupGatewayAdapter implements BankAccountLookupGateway {

    private final BankAccountQuery bankAccounts;

    @Override
    public Optional<BankAccountRef> findById(Long bankAccountId) {
        return bankAccounts.findById(bankAccountId)
                .map(a -> new BankAccountRef(a.id(), a.label(), a.active()));
    }
}
