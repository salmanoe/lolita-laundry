package id.co.lolita.laundry.order.domain.port.in;

import java.math.BigDecimal;

/**
 * A requested line on an order: an item and how many units. Price is resolved server-side
 * from the client's effective price list — never trusted from the request.
 */
public record OrderLineInput(Long itemId, BigDecimal quantity) {
}