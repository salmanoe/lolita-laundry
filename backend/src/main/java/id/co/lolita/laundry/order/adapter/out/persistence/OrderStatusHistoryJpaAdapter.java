package id.co.lolita.laundry.order.adapter.out.persistence;

import id.co.lolita.laundry.order.domain.OrderStatusHistory;
import id.co.lolita.laundry.order.domain.port.out.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class OrderStatusHistoryJpaAdapter implements OrderStatusHistoryRepository {

    private final OrderStatusHistoryJpaRepository jpaRepository;

    @Override
    public OrderStatusHistory save(OrderStatusHistory history) {
        return jpaRepository.save(OrderStatusHistoryJpaEntity.fromDomain(history)).toDomain();
    }

    @Override
    public List<OrderStatusHistory> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderIdOrderByChangedAtAsc(orderId).stream()
                .map(OrderStatusHistoryJpaEntity::toDomain).toList();
    }
}