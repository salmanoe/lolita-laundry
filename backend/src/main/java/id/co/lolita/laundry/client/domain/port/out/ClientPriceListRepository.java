package id.co.lolita.laundry.client.domain.port.out;

import id.co.lolita.laundry.client.domain.ClientPriceList;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClientPriceListRepository {
    /**
     * Returns current effective prices for all items for a given client, as of the given date.
     */
    List<ClientPriceList> findCurrentPrices(Long clientId, LocalDate asOf);

    /**
     * Returns the effective price for a specific item on a specific date (used at order creation).
     */
    Optional<ClientPriceList> findEffectivePrice(Long clientId, Long itemId, LocalDate asOf);

    /**
     * The exact price-history row for a client/item on a specific effective date, if one exists.
     * Backs the "correct this date's price in place" path (avoids the UNIQUE collision).
     */
    Optional<ClientPriceList> findExact(Long clientId, Long itemId, LocalDate effectiveDate);

    ClientPriceList save(ClientPriceList entry);

    /**
     * Removes all price-history rows for one item for a client (the item is no longer offered to
     * that client). Existing orders are unaffected — they carry a frozen price snapshot.
     */
    void deleteByClientAndItem(Long clientId, Long itemId);
}
