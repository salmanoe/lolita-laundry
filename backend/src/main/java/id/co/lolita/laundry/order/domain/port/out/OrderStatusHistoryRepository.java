package id.co.lolita.laundry.order.domain.port.out;

import id.co.lolita.laundry.order.domain.OrderStatusHistory;

import java.util.List;

public interface OrderStatusHistoryRepository {

    OrderStatusHistory save(OrderStatusHistory history);

    /**
     * History entries for an order, oldest first.
     */
    List<OrderStatusHistory> findByOrderId(Long orderId);
}
