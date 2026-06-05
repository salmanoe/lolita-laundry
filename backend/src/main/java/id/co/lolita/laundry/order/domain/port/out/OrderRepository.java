package id.co.lolita.laundry.order.domain.port.out;

import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderQuery;
import id.co.lolita.laundry.shared.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    /**
     * Loads an order with its line items eagerly populated.
     */
    Optional<Order> findById(Long id);

    Page<Order> findAll(OrderQuery query);

    /**
     * A driver's open assignments — orders assigned to them that are not yet DELIVERED.
     */
    List<Order> findActiveAssignments(Long driverId);

    /**
     * Count of orders for a client on a date — drives the per-client-per-day sequence number.
     */
    long countByClientIdAndOrderDate(Long clientId, LocalDate orderDate);
}
