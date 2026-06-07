package id.co.lolita.laundry.billing.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface OrderInvoiceJpaRepository extends JpaRepository<OrderInvoiceJpaEntity, Long> {

    Optional<OrderInvoiceJpaEntity> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);
}