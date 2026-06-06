package id.co.lolita.laundry.order.adapter.out.persistence;

import id.co.lolita.laundry.order.domain.DeliveryConfirmation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "delivery_confirmations")
@Getter
@Setter
@NoArgsConstructor
class DeliveryConfirmationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "delivered_at", nullable = false)
    private Instant deliveredAt;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "deliverer_name", nullable = false, length = 100)
    private String delivererName;

    @Column(name = "photo_url", columnDefinition = "text")
    private String photoUrl;

    @Column(columnDefinition = "text")
    private String notes;

    static DeliveryConfirmationJpaEntity fromDomain(DeliveryConfirmation c) {
        var e = new DeliveryConfirmationJpaEntity();
        e.id = c.getId();
        e.orderId = c.getOrderId();
        e.deliveredAt = c.getDeliveredAt();
        e.recipientName = c.getRecipientName();
        e.delivererName = c.getDelivererName();
        e.photoUrl = c.getPhotoUrl();
        e.notes = c.getNotes();
        return e;
    }

    DeliveryConfirmation toDomain() {
        return new DeliveryConfirmation(id, orderId, deliveredAt, recipientName, delivererName, photoUrl, notes);
    }
}