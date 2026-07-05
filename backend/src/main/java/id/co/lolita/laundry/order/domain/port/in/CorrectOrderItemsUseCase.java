package id.co.lolita.laundry.order.domain.port.in;

import id.co.lolita.laundry.order.domain.Order;

import java.util.List;

/**
 * SUPER_ADMIN correction of an order's line items on a <em>locked</em> order (DONE/DELIVERED),
 * the affordance for fixing a wrong item picked by DAILY_STAFF after the normal RECEIVED/PROCESSING
 * edit window has closed. Rejected once the order sits on an ISSUED/PAID billing (that invoice has
 * been sent). Re-prices the lines at the frozen order date, re-syncs the monthly billing, and
 * re-renders the frozen per-order invoice when the order is already DELIVERED.
 */
public interface CorrectOrderItemsUseCase {

    record CorrectOrderItemsCommand(Long orderId, Long byUserId, List<OrderLineInput> items) {
    }

    Order correctItems(CorrectOrderItemsCommand command);
}
