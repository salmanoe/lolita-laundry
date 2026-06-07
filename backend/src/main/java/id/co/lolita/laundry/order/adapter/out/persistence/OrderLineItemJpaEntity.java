package id.co.lolita.laundry.order.adapter.out.persistence;

import id.co.lolita.laundry.order.domain.OrderLineItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_line_items")
@Getter
@Setter
@NoArgsConstructor
class OrderLineItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(name = "price_at_order", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtOrder;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "department_id")
    private Long departmentId;

    static OrderLineItemJpaEntity fromDomain(OrderLineItem li) {
        var e = new OrderLineItemJpaEntity();
        e.id = li.id();
        e.itemId = li.itemId();
        e.quantity = li.quantity();
        e.priceAtOrder = li.priceAtOrder();
        e.subtotal = li.subtotal();
        e.departmentId = li.departmentId();
        return e;
    }

    OrderLineItem toDomain() {
        return new OrderLineItem(id, itemId, quantity, priceAtOrder, subtotal, departmentId);
    }
}
