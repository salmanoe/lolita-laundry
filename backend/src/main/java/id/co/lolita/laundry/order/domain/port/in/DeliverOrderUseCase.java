package id.co.lolita.laundry.order.domain.port.in;

import id.co.lolita.laundry.order.domain.DeliveryConfirmation;

/**
 * Confirms delivery of an order at {@code DONE}: stores the mandatory photo, records the
 * recipient/deliverer, and advances the order to {@code DELIVERED}.
 */
public interface DeliverOrderUseCase {

    record DeliverOrderCommand(
            Long orderId,
            String recipientName,
            String delivererName,
            String notes,
            byte[] photo,
            String photoContentType,
            String photoFilename,
            Long byUserId
    ) {
    }

    DeliveryConfirmation deliver(DeliverOrderCommand command);
}