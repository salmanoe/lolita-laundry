package id.co.lolita.laundry.order.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Order module's view of client pricing. Used to snapshot each line's price at order
 * creation ({@link #effectivePrice}) and to render prices on the public form
 * ({@link #currentPrices}).
 */
public interface PricingGateway {

    record ItemPrice(Long itemId, BigDecimal pricePerUnit) {
    }

    Optional<BigDecimal> effectivePrice(Long clientId, Long itemId, LocalDate asOf);

    List<ItemPrice> currentPrices(Long clientId);
}