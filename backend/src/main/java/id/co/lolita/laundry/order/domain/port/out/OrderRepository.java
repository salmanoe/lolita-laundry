package id.co.lolita.laundry.order.domain.port.out;

import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderQuery;
import id.co.lolita.laundry.order.domain.OrderStatus;
import id.co.lolita.laundry.shared.Page;

import java.time.LocalDate;
import java.util.Collection;
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
     * The highest existing order number whose number begins with {@code prefix}
     * ({@code CODE-yyyymmdd-}) — drives the per-client-per-day sequence number. Keyed on the
     * (frozen) order NUMBER, not the mutable {@code order_date}, so a SUPER_ADMIN date correction
     * never makes a later order reuse an already-taken number. Empty when no order carries the
     * prefix yet. Because {@code seq} is fixed-width, lexical max equals numeric max.
     */
    Optional<String> findMaxOrderNumberByPrefix(String prefix);

    /**
     * Every DELIVERED order for a client with an order date in {@code [from, to]} (a calendar
     * month), ordered by order date ascending. Backs monthly billing aggregation.
     */
    List<Order> findDeliveredByClientAndPeriod(Long clientId, LocalDate from, LocalDate to);

    /**
     * Every billable (not CANCELLED) order for a client with an order date in {@code [from, to]},
     * ordered by order date ascending. Backs the auto-built / rebuilt monthly billing.
     */
    List<Order> findBillableByClientAndPeriod(Long clientId, LocalDate from, LocalDate to);

    /**
     * Every billable (not CANCELLED) order across all clients with an order date in
     * {@code [from, to]}, ordered by order date ascending. Backs the Phase 4 dashboard/reports.
     */
    List<Order> findBillableInPeriod(LocalDate from, LocalDate to);

    /**
     * Count of orders placed on {@code date} (by order date), all clients.
     */
    long countByOrderDate(LocalDate date);

    /**
     * Count of orders currently in any of the given statuses, all clients.
     */
    long countByStatuses(Collection<OrderStatus> statuses);
}
