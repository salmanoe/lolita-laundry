package id.co.lolita.laundry.order.domain;

import lombok.Getter;

import java.time.Instant;

/**
 * Proof of delivery for an order: who received it (hotel side), who delivered it (Lolita
 * driver), and a mandatory photo. Created when an order at {@code DONE} is confirmed
 * delivered. One per order.
 */
@Getter
public class DeliveryConfirmation {

    private final Long id;
    private final Long orderId;
    private final Instant deliveredAt;
    private final String recipientName;   // Nama Penerima
    private final String delivererName;   // Nama Pengantar
    private final String photoUrl;        // storage object key — mandatory
    private final String notes;

    public DeliveryConfirmation(Long id, Long orderId, Instant deliveredAt, String recipientName,
                                String delivererName, String photoUrl, String notes) {
        this.id = id;
        this.orderId = orderId;
        this.deliveredAt = deliveredAt;
        this.recipientName = recipientName;
        this.delivererName = delivererName;
        this.photoUrl = photoUrl;
        this.notes = notes;
    }

    public static DeliveryConfirmation create(Long orderId, String recipientName, String delivererName,
                                              String photoUrl, String notes, Instant deliveredAt) {
        if (recipientName == null || recipientName.isBlank()) {
            throw new IllegalArgumentException("Recipient name (Nama Penerima) is required");
        }
        if (delivererName == null || delivererName.isBlank()) {
            throw new IllegalArgumentException("Deliverer name (Nama Pengantar) is required");
        }
        if (photoUrl == null || photoUrl.isBlank()) {
            throw new IllegalArgumentException("A delivery photo is required");
        }
        return new DeliveryConfirmation(null, orderId, deliveredAt, recipientName, delivererName, photoUrl, notes);
    }
}