package id.co.lolita.laundry.order.adapter.out.persistence;

import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderQuery;
import id.co.lolita.laundry.order.domain.OrderStatus;
import id.co.lolita.laundry.order.domain.port.out.OrderRepository;
import id.co.lolita.laundry.shared.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
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
    public List<Order> findOpenDeliveries() {
        return jpaRepository.findOpenDeliveries().stream()
                .map(OrderJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<String> findMaxOrderNumberByPrefix(String prefix) {
        return Optional.ofNullable(jpaRepository.findMaxOrderNumberByPrefix(prefix + "%"));
    }

    @Override
    public List<Order> findDeliveredByClientAndPeriod(Long clientId, LocalDate from, LocalDate to) {
        return jpaRepository.findDeliveredByClientAndPeriod(clientId, from, to).stream()
                .map(OrderJpaEntity::toDomain).toList();
    }

    @Override
    public List<Order> findBillableByClientAndPeriod(Long clientId, LocalDate from, LocalDate to) {
        return jpaRepository.findBillableByClientAndPeriod(clientId, from, to).stream()
                .map(OrderJpaEntity::toDomain).toList();
    }

    @Override
    public List<Order> findBillableInPeriod(LocalDate from, LocalDate to) {
        return jpaRepository.findBillableInPeriod(from, to).stream()
                .map(OrderJpaEntity::toDomain).toList();
    }

    @Override
    public long countByOrderDate(LocalDate date) {
        return jpaRepository.countByOrderDate(date);
    }

    @Override
    public long countByStatuses(Collection<OrderStatus> statuses) {
        return jpaRepository.countByStatusIn(statuses);
    }
}