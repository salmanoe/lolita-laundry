package id.co.lolita.laundry.order.domain.port.in;

import id.co.lolita.laundry.order.domain.Order;

import java.time.LocalDate;
import java.util.List;

/**
 * Edits a not-yet-processed order (status {@code RECEIVED}). Supplying {@code items}
 * replaces and re-prices all line items.
 */
public interface UpdateOrderUseCase {

    record UpdateOrderCommand(
            Long orderId,
            LocalDate dueDate,
            String notes,
            List<OrderLineInput> items   // null/empty = leave line items unchanged
    ) {
    }

    Order updateOrder(UpdateOrderCommand command);
}