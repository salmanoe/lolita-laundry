package id.co.lolita.laundry.order.domain.port.in;

import id.co.lolita.laundry.order.domain.Order;

import java.time.LocalDate;
import java.util.List;

/**
 * Creation of an order by authenticated Lolita staff (OWNER/STAFF).
 */
public interface CreateOrderUseCase {

    record CreateOrderCommand(
            Long clientId,
            Long departmentId,     // required iff the client bills per department
            boolean treatment,
            LocalDate dueDate,
            String submittedByName,
            String notes,
            Long createdByUserId,
            List<OrderLineInput> items
    ) {
    }

    Order createOrder(CreateOrderCommand command);
}