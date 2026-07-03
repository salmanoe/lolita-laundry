package id.co.lolita.laundry.order.domain.port.in;

import id.co.lolita.laundry.order.domain.Order;

import java.time.LocalDate;
import java.util.List;

/**
 * Edits an in-flight order (status {@code RECEIVED} or {@code PROCESSING}). Supplying
 * {@code items} replaces and re-prices all line items.
 */
public interface UpdateOrderUseCase {

    record UpdateOrderCommand(
            Long orderId,
            LocalDate orderDate,         // null = leave the order date unchanged (SUPER_ADMIN-only correction)
            Boolean treatment,           // null = leave the Treatment flag unchanged (SUPER_ADMIN-only correction)
            LocalDate dueDate,
            String notes,
            List<OrderLineInput> items   // null/empty = leave line items unchanged
    ) {
    }

    Order updateOrder(UpdateOrderCommand command);
}