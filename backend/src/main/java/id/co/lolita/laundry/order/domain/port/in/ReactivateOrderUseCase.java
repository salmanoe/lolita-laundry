package id.co.lolita.laundry.order.domain.port.in;

import id.co.lolita.laundry.order.domain.Order;

/**
 * Reverses an order cancellation (SUPER_ADMIN correction), restoring the status the order held
 * before it was cancelled and re-adding it to the client's monthly billing. Only a {@code CANCELLED}
 * order can be reactivated.
 */
public interface ReactivateOrderUseCase {

    record ReactivateOrderCommand(Long orderId, Long byUserId) {
    }

    Order reactivate(ReactivateOrderCommand command);
}
