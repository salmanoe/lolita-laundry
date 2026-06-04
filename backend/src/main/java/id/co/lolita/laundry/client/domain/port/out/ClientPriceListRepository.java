package id.co.lolita.laundry.client.domain.port.out;

import id.co.lolita.laundry.client.domain.ClientPriceList;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClientPriceListRepository {
    /**
     * Returns current effective prices for all items for a given client (as of today).
     */
    List<ClientPriceList> findCurrentPrices(Long clientId);

    /**
     * Returns the effective price for a specific item on a specific date (used at order creation).
     */
    Optional<ClientPriceList> findEffectivePrice(Long clientId, Long itemId, LocalDate asOf);

    ClientPriceList save(ClientPriceList entry);
}
