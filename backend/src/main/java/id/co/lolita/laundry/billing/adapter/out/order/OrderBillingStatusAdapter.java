package id.co.lolita.laundry.billing.adapter.out.order;

import id.co.lolita.laundry.billing.domain.BillingStatus;
import id.co.lolita.laundry.billing.domain.port.out.MonthlyBillingRepository;
import id.co.lolita.laundry.order.domain.port.out.billing.BillingStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Provides the order module's {@link BillingStatusPort} SPI (named interface
 * {@code order::billing-spi}) from billing's own repository. The import edge is billing → order,
 * matching the existing direction, so no Modulith cycle forms.
 */
@Component
@RequiredArgsConstructor
class OrderBillingStatusAdapter implements BillingStatusPort {

    private final MonthlyBillingRepository billingRepository;

    @Override
    public boolean isOrderOnIssuedBilling(Long orderId) {
        return billingRepository.findAllByOrderLine(orderId).stream()
                .anyMatch(b -> b.getStatus() != BillingStatus.DRAFT);
    }
}
