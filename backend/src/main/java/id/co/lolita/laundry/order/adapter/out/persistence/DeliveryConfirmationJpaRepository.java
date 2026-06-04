package id.co.lolita.laundry.order.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface DeliveryConfirmationJpaRepository extends JpaRepository<DeliveryConfirmationJpaEntity, Long> {

    Optional<DeliveryConfirmationJpaEntity> findByOrderId(Long orderId);
}