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
     * The open delivery pool — every order not yet DELIVERED, shared across all drivers.
     * Ready (DONE) orders first, then by oldest order date.
     */
    List<Order> findOpenDeliveries();

    /**
     * Count of orders for a client on a date — drives the per-client-per-day sequence number.
     */
    long countByClientIdAndOrderDate(Long clientId, LocalDate orderDate);

    /**
     * Every DELIVERED order for a client with an order date in {@code [from, to]} (a calendar
     * month), ordered by order date ascending. Backs monthly billing aggregation.
     */
    List<Order> findDeliveredByClientAndPeriod(Long clientId, LocalDate from, LocalDate to);
}
