package id.co.lolita.laundry.client.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A price entry for a specific item for a specific client, effective from a given date.
 *
 * <p>Rows are append-only — never updated or deleted. To change a price, insert a new row
 * with a newer {@code effectiveDate}. Historical prices on existing orders are preserved.
 *
 * <p>Lookup: {@code WHERE client_id = ? AND item_id = ? AND effective_date <= :order_date
 * ORDER BY effective_date DESC LIMIT 1}
 */
public record ClientPriceList(
        Long id,
        Long clientId,
        Long itemId,
        BigDecimal pricePerUnit,
        LocalDate effectiveDate,
        Instant createdAt
) {
}
