package id.co.lolita.laundry.order.domain.port.in;

import id.co.lolita.laundry.order.domain.Order;

/**
 * Cancels (voids) an order. Allowed while {@code RECEIVED}, {@code PROCESSING} or {@code DONE};
 * a delivered order cannot be cancelled. A cancelled order is removed from the client's monthly
 * billing.
 */
public interface CancelOrderUseCase {

    record CancelOrderCommand(Long orderId, Long byUserId, String notes) {
    }

    Order cancel(CancelOrderCommand command);
}
