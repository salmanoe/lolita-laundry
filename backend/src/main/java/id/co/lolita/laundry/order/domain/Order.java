package id.co.lolita.laundry.order.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Order aggregate root. Owns its line items and enforces the one-way status lifecycle.
 *
 * <p>Created via {@link #create} (status starts at {@code RECEIVED}); rehydrated from
 * persistence via the all-args constructor. Business rules — status transitions, line
 * pricing, edit window — live here, not in the application service.
 */
@Getter
public class Order {

    private final Long id;
    private final String orderNumber;
    private final Long clientId;
    private final Long departmentId;       // nullable — only set for PER_DEPARTMENT clients
    private final LocalDate orderDate;
    private LocalDate dueDate;
    private OrderStatus status;
    private final BigDecimal pricingMultiplier;   // 1.0 Reguler, 2.0 Treatment (PBS)
    private final String submittedByName;
    private String notes;
    private final Long createdByUserId;    // nullable — null when submitted via public token
    private Long assignedDriverId;         // nullable — set when staff assign the order to a driver
    private final Instant createdAt;
    private final List<OrderLineItem> lineItems = new ArrayList<>();

    /**
     * Input shape for a new line before pricing/subtotal computation.
     */
    public record NewLine(Long itemId, BigDecimal quantity, BigDecimal priceAtOrder) {
    }

    public Order(Long id, String orderNumber, Long clientId, Long departmentId, LocalDate orderDate,
                 LocalDate dueDate, OrderStatus status, BigDecimal pricingMultiplier, String submittedByName,
                 String notes, Long createdByUserId, Long assignedDriverId, Instant createdAt,
                 List<OrderLineItem> lineItems) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.clientId = clientId;
        this.departmentId = departmentId;
        this.orderDate = orderDate;
        this.dueDate = dueDate;
        this.status = status;
        this.pricingMultiplier = pricingMultiplier;
        this.submittedByName = submittedByName;
        this.notes = notes;
        this.createdByUserId = createdByUserId;
        this.assignedDriverId = assignedDriverId;
        this.createdAt = createdAt;
        if (lineItems != null) {
            this.lineItems.addAll(lineItems);
        }
    }

    public static Order create(String orderNumber, Long clientId, Long departmentId, LocalDate orderDate,
                               LocalDate dueDate, BigDecimal pricingMultiplier, String submittedByName,
                               String notes, Long createdByUserId, List<NewLine> lines, Instant createdAt) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one line item");
        }
        var order = new Order(null, orderNumber, clientId, departmentId, orderDate, dueDate,
                OrderStatus.RECEIVED, pricingMultiplier, submittedByName, notes, createdByUserId, null,
                createdAt, null);
        lines.forEach(l -> order.lineItems.add(
                OrderLineItem.create(l.itemId(), l.quantity(), l.priceAtOrder(), pricingMultiplier)));
        return order;
    }

    /**
     * Assigns this order to a driver (or unassigns when {@code driverId} is null). The driver
     * confirms delivery from their own screen. Rejected once the order is {@code DELIVERED} —
     * there is nothing left to deliver. The driver's existence/role is validated upstream.
     */
    public void assignDriver(Long driverId) {
        if (status == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("A delivered order can no longer be assigned");
        }
        this.assignedDriverId = driverId;
    }

    /**
     * Advances the status by exactly one step. Rejects skips, reversals, and no-ops.
     */
    public void advanceStatus(OrderStatus target) {
        if (!status.canAdvanceTo(target)) {
            throw new IllegalArgumentException(
                    "Cannot change order status from %s to %s".formatted(status, target));
        }
        this.status = target;
    }

    /**
     * Edits an in-flight order. Line items, if supplied, are fully replaced and re-priced
     * with the order's multiplier. Allowed only while {@code RECEIVED} or {@code PROCESSING}
     * — once {@code DONE} or {@code DELIVERED} the order is locked.
     */
    public void edit(LocalDate dueDate, String notes, List<NewLine> lines) {
        if (status != OrderStatus.RECEIVED && status != OrderStatus.PROCESSING) {
            throw new IllegalArgumentException("Order can only be edited while RECEIVED or PROCESSING");
        }
        this.dueDate = dueDate;
        this.notes = notes;
        if (lines != null && !lines.isEmpty()) {
            this.lineItems.clear();
            lines.forEach(l -> this.lineItems.add(
                    OrderLineItem.create(l.itemId(), l.quantity(), l.priceAtOrder(), pricingMultiplier)));
        }
    }

    /**
     * Read-only view of the order's line items.
     */
    public List<OrderLineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    public BigDecimal total() {
        return lineItems.stream()
                .map(OrderLineItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}