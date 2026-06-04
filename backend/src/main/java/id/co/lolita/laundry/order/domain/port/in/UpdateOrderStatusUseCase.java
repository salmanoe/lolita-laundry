package id.co.lolita.laundry.order.domain.port.in;

import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderStatus;

/**
 * Advances an order one step along the lifecycle. The {@code DELIVERED} transition is NOT
 * permitted here — it happens only through {@link DeliverOrderUseCase}, which captures the
 * mandatory delivery proof.
 */
public interface UpdateOrderStatusUseCase {

    record AdvanceStatusCommand(Long orderId, OrderStatus target, Long byUserId, String notes) {
    }

    Order advanceStatus(AdvanceStatusCommand command);
}