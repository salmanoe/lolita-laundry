package id.co.lolita.laundry.order.adapter.out.persistence;

import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderQuery;
import id.co.lolita.laundry.order.domain.port.out.OrderRepository;
import id.co.lolita.laundry.shared.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class OrderJpaAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity;
        if (order.getId() == null) {
            entity = OrderJpaEntity.newFromDomain(order);
        } else {
            // Load the managed entity so scalar updates and line-item reconciliation
            // (orphan removal) happen within the persistence context.
            entity = jpaRepository.findById(order.getId())
                    .orElseThrow(() -> new IllegalStateException("Order vanished while saving: " + order.getId()));
            entity.applyScalars(order);
            entity.reconcileLineItems(order.getLineItems());
        }
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpaRepository.findById(id).map(OrderJpaEntity::toDomain);
    }

    @Override
    public Page<Order> findAll(OrderQuery query) {
        var springPage = jpaRepository.search(
                query.clientId(), query.status(), query.from(), query.to(),
                PageMapper.toPageable(query.page()));
        return new Page<>(
                springPage.getContent().stream().map(OrderJpaEntity::toDomain).toList(),
                springPage.getNumber(), springPage.getSize(),
                springPage.getTotalElements(), springPage.getTotalPages()
        );
    }

    @Override
    public long countByClientIdAndOrderDate(Long clientId, LocalDate orderDate) {
        return jpaRepository.countByClientIdAndOrderDate(clientId, orderDate);
    }
}