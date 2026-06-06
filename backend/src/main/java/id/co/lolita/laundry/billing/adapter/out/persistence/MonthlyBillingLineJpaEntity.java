package id.co.lolita.laundry.billing.adapter.out.persistence;

import id.co.lolita.laundry.billing.domain.MonthlyBillingLine;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "monthly_billing_lines")
@Getter
@Setter
@NoArgsConstructor
class MonthlyBillingLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", nullable = false)
    private MonthlyBillingJpaEntity billing;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    static MonthlyBillingLineJpaEntity fromDomain(MonthlyBillingLine l) {
        var e = new MonthlyBillingLineJpaEntity();
        e.id = l.id();
        e.orderId = l.orderId();
        e.orderNumber = l.orderNumber();
        e.orderDate = l.orderDate();
        e.subtotal = l.subtotal();
        return e;
    }

    MonthlyBillingLine toDomain() {
        return new MonthlyBillingLine(id, orderId, orderNumber, orderDate, subtotal);
    }
}