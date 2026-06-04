package id.co.lolita.laundry.order.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface OrderStatusHistoryJpaRepository extends JpaRepository<OrderStatusHistoryJpaEntity, Long> {

    List<OrderStatusHistoryJpaEntity> findByOrderIdOrderByChangedAtAsc(Long orderId);
}