package id.co.lolita.laundry.billing.adapter.out.persistence;

import id.co.lolita.laundry.billing.domain.OrderInvoice;
import id.co.lolita.laundry.billing.domain.port.out.OrderInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class OrderInvoiceJpaAdapter implements OrderInvoiceRepository {

    private final OrderInvoiceJpaRepository jpaRepository;

    @Override
    public OrderInvoice save(OrderInvoice invoice) {
        return jpaRepository.save(OrderInvoiceJpaEntity.fromDomain(invoice)).toDomain();
    }

    @Override
    public Optional<OrderInvoice> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId).map(OrderInvoiceJpaEntity::toDomain);
    }

    @Override
    public boolean existsByOrderId(Long orderId) {
        return jpaRepository.existsByOrderId(orderId);
    }

    @Override
    public List<OrderInvoice> findAll() {
        return jpaRepository.findAll().stream().map(OrderInvoiceJpaEntity::toDomain).toList();
    }
}