package id.co.lolita.laundry.order.domain.port.in;

import id.co.lolita.laundry.order.domain.DeliveryConfirmation;
import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderQuery;
import id.co.lolita.laundry.order.domain.OrderStatusHistory;
import id.co.lolita.laundry.shared.Page;

import java.util.List;
import java.util.Optional;

public interface GetOrdersUseCase {

    Page<Order> getOrders(OrderQuery query);

    Order getById(Long id);

    List<OrderStatusHistory> getHistory(Long orderId);

    Optional<DeliveryConfirmation> getDelivery(Long orderId);

    /**
     * Short-lived pre-signed URL for the delivery photo, so the frontend can render it directly.
     */
    Optional<String> getDeliveryPhotoUrl(Long orderId);
}