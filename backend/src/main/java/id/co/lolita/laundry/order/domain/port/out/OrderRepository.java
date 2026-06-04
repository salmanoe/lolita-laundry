package id.co.lolita.laundry.order.domain.port.out;

import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderQuery;
import id.co.lolita.laundry.shared.Page;

import java.time.LocalDate;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    /** Loads an order with its line items eagerly populated. */
    Optional<Order> findById(Long id);

    Page<Order> findAll(OrderQuery query);

    /** Count of orders for a client on a date — drives the per-client-per-day sequence number. */
    long countByClientIdAndOrderDate(Long clientId, LocalDate orderDate);
}
