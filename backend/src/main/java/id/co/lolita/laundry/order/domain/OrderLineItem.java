package id.co.lolita.laundry.order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A single line on an order — immutable once created.
 *
 * <p>{@code priceAtOrder} is a snapshot of the client's effective unit price at order
 * creation and is never updated afterward. {@code subtotal = quantity × priceAtOrder ×
 * pricingMultiplier}, rounded to 2 decimals (matches the {@code order_line_items.subtotal}
 * column comment in V1).
 *
 * <p>{@code departmentId} is a snapshot of the item's department for the client at order
 * creation (PER_DEPARTMENT clients only; null for COMBINED). An order may therefore span
 * several departments — the monthly billing splits it per department on this field.
 *
 * @param departmentId nullable — only set for PER_DEPARTMENT clients
 */
public record OrderLineItem(
        Long id,
        Long itemId,
        BigDecimal quantity,
        BigDecimal priceAtOrder,
        BigDecimal subtotal,
        Long departmentId) {

    /**
     * Builds a new (unsaved) line, computing the subtotal with the order's pricing multiplier.
     */
    static OrderLineItem create(Long itemId, BigDecimal quantity, BigDecimal priceAtOrder,
                                Long departmentId, BigDecimal multiplier) {
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
        return new OrderLineItem(null, itemId, quantity, priceAtOrder, computed, departmentId);
    }
}
