package id.co.lolita.laundry.order.adapter.out.persistence;

import id.co.lolita.laundry.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    long countByClientIdAndOrderDate(Long clientId, LocalDate orderDate);

    /**
     * DELIVERED orders for a client whose order date falls in {@code [from, to]} — the
     * monthly billing aggregation set. Oldest first.
     */
    @Query("""
            SELECT o FROM OrderJpaEntity o
            WHERE o.clientId = :clientId
              AND o.status = id.co.lolita.laundry.order.domain.OrderStatus.DELIVERED
              AND o.orderDate >= :from
              AND o.orderDate <= :to
            ORDER BY o.orderDate ASC, o.id ASC
            """)
    List<OrderJpaEntity> findDeliveredByClientAndPeriod(
            @Param("clientId") Long clientId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Billable orders for a client in {@code [from, to]} — every order in the period that is
     * not CANCELLED (any other status counts). Backs the auto-built monthly billing. Oldest first.
     */
    @Query("""
            SELECT o FROM OrderJpaEntity o
            WHERE o.clientId = :clientId
              AND o.status <> id.co.lolita.laundry.order.domain.OrderStatus.CANCELLED
              AND o.orderDate >= :from
              AND o.orderDate <= :to
            ORDER BY o.orderDate ASC, o.id ASC
            """)
    List<OrderJpaEntity> findBillableByClientAndPeriod(
            @Param("clientId") Long clientId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * The open delivery pool — every order not yet DELIVERED, shared across all drivers.
     * Ordered ready-first (DONE on top), then by oldest order date so the longest-waiting
     * deliveries surface first.
     */
    @Query("""
            SELECT o FROM OrderJpaEntity o
            WHERE o.status <> id.co.lolita.laundry.order.domain.OrderStatus.DELIVERED
              AND o.status <> id.co.lolita.laundry.order.domain.OrderStatus.CANCELLED
            ORDER BY CASE WHEN o.status = id.co.lolita.laundry.order.domain.OrderStatus.DONE THEN 0 ELSE 1 END,
                     o.orderDate ASC
            """)
    List<OrderJpaEntity> findOpenDeliveries();

    /**
     * Lists orders with optional filters — any null parameter drops its constraint.
     */
    @Query("""
            SELECT o FROM OrderJpaEntity o
            WHERE (:clientId is null or o.clientId = :clientId)
              AND (:status   is null or o.status   = :status)
              AND (:from     is null or o.orderDate >= :from)
              AND (:to       is null or o.orderDate <= :to)
            """)
    Page<OrderJpaEntity> search(
            @Param("clientId") Long clientId,
            @Param("status") OrderStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable
    );
}