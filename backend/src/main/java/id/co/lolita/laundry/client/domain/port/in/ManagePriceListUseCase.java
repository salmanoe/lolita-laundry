package id.co.lolita.laundry.client.domain.port.in;

import id.co.lolita.laundry.client.domain.ClientPriceList;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ManagePriceListUseCase {

    /**
     * @param departmentId required for PER_DEPARTMENT clients (the "Atur Harga" item→department
     *                     assignment), must be null for COMBINED clients.
     */
    record SetPriceCommand(Long clientId, Long itemId, BigDecimal pricePerUnit, LocalDate effectiveDate,
                           Long departmentId) {
    }

    /**
     * A client's current effective price for an item, plus its department assignment (null for
     * COMBINED clients or unassigned items).
     */
    record CurrentPrice(Long itemId, BigDecimal pricePerUnit, LocalDate effectiveDate, Long departmentId) {
    }

    List<CurrentPrice> getCurrentPrices(Long clientId);

    ClientPriceList setPrice(SetPriceCommand command);

    /**
     * Removes an item from a client's price list entirely (all price-history rows + the
     * item→department mapping). Existing orders keep their frozen price snapshot.
     */
    void removeItemPricing(Long clientId, Long itemId);
}