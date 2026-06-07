package id.co.lolita.laundry.order.adapter.out.gateway;

import id.co.lolita.laundry.client.domain.port.in.ClientPricingQuery;
import id.co.lolita.laundry.order.domain.port.out.PricingGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PricingGatewayAdapter implements PricingGateway {

    private final ClientPricingQuery pricing;

    @Override
    public Optional<BigDecimal> effectivePrice(Long clientId, Long itemId, LocalDate asOf) {
        return pricing.effectivePrice(clientId, itemId, asOf);
    }

    @Override
    public List<ItemPrice> currentPrices(Long clientId) {
        return pricing.currentPrices(clientId).stream()
                .map(p -> new ItemPrice(p.itemId(), p.pricePerUnit()))
                .toList();
    }

    @Override
    public List<ItemDepartment> itemDepartments(Long clientId) {
        return pricing.itemDepartments(clientId).stream()
                .map(d -> new ItemDepartment(d.itemId(), d.departmentId()))
                .toList();
    }

    @Override
    public Optional<Long> departmentForItem(Long clientId, Long itemId) {
        return pricing.departmentForItem(clientId, itemId);
    }
}