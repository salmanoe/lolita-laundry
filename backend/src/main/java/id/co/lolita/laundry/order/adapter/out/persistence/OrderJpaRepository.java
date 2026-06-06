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
     * The open delivery pool — every order not yet DELIVERED, shared across all drivers.
     * Ordered ready-first (DONE on top), then by oldest order date so the longest-waiting
     * deliveries surface first.
     */
    @Query("""
            SELECT o FROM OrderJpaEntity o
            WHERE o.status <> id.co.lolita.laundry.order.domain.OrderStatus.DELIVERED
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