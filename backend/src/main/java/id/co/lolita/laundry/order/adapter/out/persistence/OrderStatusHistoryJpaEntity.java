package id.co.lolita.laundry.order.adapter.out.persistence;

import id.co.lolita.laundry.order.domain.OrderStatus;
import id.co.lolita.laundry.order.domain.OrderStatusHistory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "order_status_history")
@Getter
@Setter
@NoArgsConstructor
class OrderStatusHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 12)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 12)
    private OrderStatus toStatus;

    @Column(name = "changed_by_id")
    private Long changedByUserId;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(columnDefinition = "text")
    private String notes;

    static OrderStatusHistoryJpaEntity fromDomain(OrderStatusHistory h) {
        var e = new OrderStatusHistoryJpaEntity();
        e.id = h.id();
        e.orderId = h.orderId();
        e.fromStatus = h.fromStatus();
        e.toStatus = h.toStatus();
        e.changedByUserId = h.changedByUserId();
        e.changedAt = h.changedAt();
        e.notes = h.notes();
        return e;
    }

    OrderStatusHistory toDomain() {
        return new OrderStatusHistory(id, orderId, fromStatus, toStatus, changedByUserId, changedAt, notes);
    }
}