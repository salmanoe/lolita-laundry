package id.co.lolita.laundry.order.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A single line on an order — immutable once created.
 *
 * <p>{@code priceAtOrder} is a snapshot of the client's effective unit price at order
 * creation and is never updated afterwards. {@code subtotal = quantity × priceAtOrder ×
 * pricingMultiplier}, rounded to 2 decimals (matches the {@code order_line_items.subtotal}
 * column comment in V1).
 */
@Getter
public class OrderLineItem {

    private final Long id;
    private final Long itemId;
    private final BigDecimal quantity;
    private final BigDecimal priceAtOrder;
    private final BigDecimal subtotal;

    public OrderLineItem(Long id, Long itemId, BigDecimal quantity, BigDecimal priceAtOrder, BigDecimal subtotal) {
        this.id = id;
        this.itemId = itemId;
        this.quantity = quantity;
        this.priceAtOrder = priceAtOrder;
        this.subtotal = subtotal;
    }

    /** Builds a new (unsaved) line, computing the subtotal with the order's pricing multiplier. */
    static OrderLineItem create(Long itemId, BigDecimal quantity, BigDecimal priceAtOrder, BigDecimal multiplier) {
        if (itemId == null) {
            throw new IllegalArgumentException("Line item must reference an item");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Line item quantity must be positive");
        }
        if (priceAtOrder == null || priceAtOrder.signum() < 0) {
            throw new IllegalArgumentException("Line item price must be zero or positive");
        }
        var computed = quantity.multiply(priceAtOrder).multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        return new OrderLineItem(null, itemId, quantity, priceAtOrder, computed);
    }
}
