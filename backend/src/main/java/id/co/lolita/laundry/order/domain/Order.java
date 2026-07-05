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
    // Not final: SUPER_ADMIN may correct the order date via edit(). The order NUMBER stays frozen
    // (it is an identity referenced by the invoice number and billing lines), so a corrected date
    // can legitimately differ from the yyyymmdd baked into the number.
    private LocalDate orderDate;
    private LocalDate dueDate;
    private OrderStatus status;
    // Not final: SUPER_ADMIN may correct the Treatment flag via edit() (a whole-order pricing
    // correction). Changing it re-prices every existing line — the multiplier is baked into each
    // OrderLineItem.subtotal — so a correction reshuffles the monthly billing exactly like a
    // quantity edit does. 1.0 Reguler, 2.0 Treatment (PBS).
    private BigDecimal pricingMultiplier;
    private final String submittedByName;
    private String notes;
    private final Long createdByUserId;    // nullable — null when submitted via public token
    private final Instant createdAt;
    private final List<OrderLineItem> lineItems = new ArrayList<>();

    /**
     * Input shape for a new line before pricing/subtotal computation. {@code departmentId} is
     * the item's department for the client (PER_DEPARTMENT clients only; null for COMBINED).
     */
    public record NewLine(Long itemId, BigDecimal quantity, BigDecimal priceAtOrder, Long departmentId) {
    }

    public Order(Long id, String orderNumber, Long clientId, LocalDate orderDate,
                 LocalDate dueDate, OrderStatus status, BigDecimal pricingMultiplier, String submittedByName,
                 String notes, Long createdByUserId, Instant createdAt,
                 List<OrderLineItem> lineItems) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.clientId = clientId;
        this.orderDate = orderDate;
        this.dueDate = dueDate;
        this.status = status;
        this.pricingMultiplier = pricingMultiplier;
        this.submittedByName = submittedByName;
        this.notes = notes;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        if (lineItems != null) {
            this.lineItems.addAll(lineItems);
        }
    }

    public static Order create(String orderNumber, Long clientId, LocalDate orderDate,
                               LocalDate dueDate, BigDecimal pricingMultiplier, String submittedByName,
                               String notes, Long createdByUserId, List<NewLine> lines, Instant createdAt) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one line item");
        }
        var order = new Order(null, orderNumber, clientId, orderDate, dueDate,
                OrderStatus.RECEIVED, pricingMultiplier, submittedByName, notes, createdByUserId,
                createdAt, null);
        lines.forEach(l -> order.lineItems.add(
                OrderLineItem.create(l.itemId(), l.quantity(), l.priceAtOrder(), l.departmentId(), pricingMultiplier)));
        return order;
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
     * Marks the order DELIVERED. Allowed from any status <em>except</em> already-DELIVERED:
     * a driver delivers what they physically have even if staff never advanced the order
     * through PROCESSING/DONE. The already-DELIVERED guard is the concurrency backstop — if two
     * drivers grab the same order, the second {@code deliver()} fails once the first commits.
     */
    public void markDelivered() {
        if (status == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Order is already delivered");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot deliver a cancelled order");
        }
        this.status = OrderStatus.DELIVERED;
    }

    /**
     * Cancels (voids) the order — a terminal off-ramp from the normal flow. Allowed from
     * {@code RECEIVED}, {@code PROCESSING} or {@code DONE}; a delivered order cannot be canceled.
     * A canceled order drops off the client's monthly billing.
     */
    public void cancel() {
        if (status == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Cannot cancel a delivered order");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order is already cancelled");
        }
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * Edits an in-flight order. Both {@code orderDate} and {@code pricingMultiplier} are
     * SUPER_ADMIN corrections (caller-gated) — a non-null value overrides, null leaves it
     * unchanged. Line items, if supplied, are fully replaced and re-priced with the (possibly
     * corrected) multiplier. A Treatment correction with <em>no</em> new line list still
     * re-prices the existing lines so their subtotals reflect the new multiplier. Allowed only
     * while {@code RECEIVED} or {@code PROCESSING} — once {@code DONE} or {@code DELIVERED} the
     * order is locked.
     */
    public void edit(LocalDate orderDate, BigDecimal pricingMultiplier, LocalDate dueDate,
                     String notes, List<NewLine> lines) {
        if (status != OrderStatus.RECEIVED && status != OrderStatus.PROCESSING) {
            throw new IllegalArgumentException("Order can only be edited while RECEIVED or PROCESSING");
        }
        if (orderDate != null) {
            this.orderDate = orderDate;
        }
        boolean multiplierChanged = pricingMultiplier != null
                && pricingMultiplier.compareTo(this.pricingMultiplier) != 0;
        if (pricingMultiplier != null) {
            this.pricingMultiplier = pricingMultiplier;
        }
        this.dueDate = dueDate;
        this.notes = notes;
        if (lines != null && !lines.isEmpty()) {
            this.lineItems.clear();
            lines.forEach(l -> this.lineItems.add(
                    OrderLineItem.create(l.itemId(), l.quantity(), l.priceAtOrder(), l.departmentId(), this.pricingMultiplier)));
        } else if (multiplierChanged) {
            // Treatment-only correction: no new line list, so re-price the existing lines in place
            // with the corrected multiplier (price snapshots are preserved; only the subtotal changes).
            var existing = List.copyOf(this.lineItems);
            this.lineItems.clear();
            existing.forEach(li -> this.lineItems.add(OrderLineItem.create(
                    li.itemId(), li.quantity(), li.priceAtOrder(), li.departmentId(), this.pricingMultiplier)));
        }
    }

    /**
     * Reverses a cancellation (SUPER_ADMIN correction), restoring the order to the status it held
     * before it was cancelled. Only a {@code CANCELLED} order can be reactivated; the target must be
     * one of the live statuses ({@code RECEIVED}/{@code PROCESSING}/{@code DONE}) — a cancel can
     * never originate from {@code DELIVERED}, so that is rejected as a target too. The caller
     * re-syncs the monthly billing (mirror of {@link #cancel()} dropping it).
     */
    public void reactivate(OrderStatus previous) {
        if (status != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Only a cancelled order can be reactivated");
        }
        if (previous == null || previous == OrderStatus.CANCELLED || previous == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Invalid reactivation target: " + previous);
        }
        this.status = previous;
    }

    /**
     * SUPER_ADMIN correction of the line items on a <em>locked</em> order — the affordance for
     * fixing a wrong item picked by DAILY_STAFF after the order has advanced past the normal edit
     * window. Unlike {@link #edit}, there is no status-window check (DONE/DELIVERED are allowed);
     * only a {@code CANCELLED} order is rejected. The lines are fully replaced and re-priced with
     * the order's existing multiplier (Treatment stays as-is — that is a separate correction). The
     * caller supplies price snapshots resolved at the frozen order date, re-syncs the monthly
     * billing, and (for a DELIVERED order) re-renders the frozen invoice.
     */
    public void correctItems(List<NewLine> lines) {
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot edit a cancelled order");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("At least one line item is required");
        }
        this.lineItems.clear();
        lines.forEach(l -> this.lineItems.add(
                OrderLineItem.create(l.itemId(), l.quantity(), l.priceAtOrder(), l.departmentId(), this.pricingMultiplier)));
    }

    /**
     * Read-only view of the order's line items.
     */
    public List<OrderLineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    public BigDecimal total() {
        return lineItems.stream()
                .map(OrderLineItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}