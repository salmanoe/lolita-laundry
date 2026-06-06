package id.co.lolita.laundry.order.adapter.out.persistence;

import id.co.lolita.laundry.order.domain.DeliveryConfirmation;
import id.co.lolita.laundry.order.domain.port.out.DeliveryConfirmationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class DeliveryConfirmationJpaAdapter implements DeliveryConfirmationRepository {

    private final DeliveryConfirmationJpaRepository jpaRepository;

    @Override
    public DeliveryConfirmation save(DeliveryConfirmation confirmation) {
        return jpaRepository.save(DeliveryConfirmationJpaEntity.fromDomain(confirmation)).toDomain();
    }

    @Override
    public Optional<DeliveryConfirmation> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId).map(DeliveryConfirmationJpaEntity::toDomain);
    }
}