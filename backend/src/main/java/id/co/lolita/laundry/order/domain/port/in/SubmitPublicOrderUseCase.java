package id.co.lolita.laundry.order.domain.port.in;

import id.co.lolita.laundry.order.domain.Order;

import java.util.List;
import java.util.UUID;

/**
 * Submission of an order through the public tokenized form (no authentication).
 */
public interface SubmitPublicOrderUseCase {

    record SubmitPublicOrderCommand(
            UUID token,
            String submittedByName,
            boolean treatment,     // 2× multiplier — allowed only for per-department clients
            String notes,
            List<OrderLineInput> items
    ) {
    }

    Order submit(SubmitPublicOrderCommand command);
}