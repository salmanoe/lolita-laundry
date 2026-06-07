package id.co.lolita.laundry.order.adapter.out.persistence;

import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderLineItem;
import id.co.lolita.laundry.order.domain.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private OrderStatus status;

    @Column(name = "pricing_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal pricingMultiplier;

    @Column(name = "submitted_by_name", nullable = false, length = 100)
    private String submittedByName;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineItemJpaEntity> lineItems = new ArrayList<>();

    /**
     * Builds a fresh, unsaved entity (used for new orders) including its line items.
     */
    static OrderJpaEntity newFromDomain(Order o) {
        var e = new OrderJpaEntity();
        e.id = o.getId();
        e.orderNumber = o.getOrderNumber();
        e.clientId = o.getClientId();
        e.orderDate = o.getOrderDate();
        e.pricingMultiplier = o.getPricingMultiplier();
        e.submittedByName = o.getSubmittedByName();
        e.createdByUserId = o.getCreatedByUserId();
        e.createdAt = o.getCreatedAt();
        e.applyScalars(o);
        e.setLineItemsFrom(o.getLineItems());
        return e;
    }

    /**
     * Copies the fields that may change over an order's life onto a managed entity.
     */
    void applyScalars(Order o) {
        this.status = o.getStatus();
        this.dueDate = o.getDueDate();
        this.notes = o.getNotes();
    }

    /**
     * Reconciles the line-item collection with the domain order. A no-op when the domain
     * still carries exactly the persisted lines (same ids — the common status/delivery
     * path); otherwise clears (orphanRemoval deletes the old rows) and rebuilds from the
     * re-priced lines (the RECEIVED edit path).
     */
    void reconcileLineItems(List<OrderLineItem> domainLines) {
        boolean unchanged = domainLines.size() == lineItems.size()
                && domainLines.stream().allMatch(d -> d.id() != null
                && lineItems.stream().anyMatch(e -> d.id().equals(e.getId())));
        if (unchanged) {
            return;
        }
        lineItems.clear();
        setLineItemsFrom(domainLines);
    }

    private void setLineItemsFrom(List<OrderLineItem> domainLines) {
        for (var d : domainLines) {
            var le = OrderLineItemJpaEntity.fromDomain(d);
            le.setOrder(this);
            lineItems.add(le);
        }
    }

    Order toDomain() {
        var lines = lineItems.stream().map(OrderLineItemJpaEntity::toDomain).toList();
        return new Order(id, orderNumber, clientId, orderDate, dueDate, status,
                pricingMultiplier, submittedByName, notes, createdByUserId, createdAt, lines);
    }
}
