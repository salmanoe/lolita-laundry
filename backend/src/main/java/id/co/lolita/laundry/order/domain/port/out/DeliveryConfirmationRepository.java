package id.co.lolita.laundry.order.domain.port.out;

import id.co.lolita.laundry.order.domain.DeliveryConfirmation;

import java.util.Optional;

public interface DeliveryConfirmationRepository {

    DeliveryConfirmation save(DeliveryConfirmation confirmation);

    Optional<DeliveryConfirmation> findByOrderId(Long orderId);
}
