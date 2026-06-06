package id.co.lolita.laundry.order.adapter.in.web.dto;

import id.co.lolita.laundry.order.domain.DeliveryConfirmation;

import java.time.Instant;

public record DeliveryConfirmationResponse(
        Long id,
        Long orderId,
        Instant deliveredAt,
        String recipientName,
        String delivererName,
        String photoUrl,
        String notes
) {
    public static DeliveryConfirmationResponse from(DeliveryConfirmation c) {
        return new DeliveryConfirmationResponse(c.getId(), c.getOrderId(), c.getDeliveredAt(),
                c.getRecipientName(), c.getDelivererName(), c.getPhotoUrl(), c.getNotes());
    }
}
