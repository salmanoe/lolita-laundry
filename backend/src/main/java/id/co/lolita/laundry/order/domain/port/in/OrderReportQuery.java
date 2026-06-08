package id.co.lolita.laundry.order.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only order aggregates that the {@code report} module needs for the owner/staff dashboard
 * and reports (Phase 4). All money is "billable" — every order whose status is not CANCELLED,
 * keyed by order date — and already includes the Treatment pricing multiplier (line subtotals
 * bake it in, so an order total is just the sum of its line subtotals).
 *
 * <p>Exposed cross-module (named interface "api"). Returns self-contained records — {@code status}
 * is a plain String, never the {@code OrderStatus} domain enum, so no order domain type leaks
 * across the boundary.
 */
public interface OrderReportQuery {

    /**
     * Operational counts for the dashboard, scoped to {@code date} for {@code ordersOnDate}.
     */
    record StatusCounts(long ordersOnDate, long inProgress, long readyForDelivery) {
    }

    /**
     * One client's billable totals over a period.
     */
    record ClientTotals(Long clientId, long orderCount, BigDecimal total) {
    }

    /**
     * A single billable order row for the per-hotel report.
     */
    record OrderRow(Long orderId, String orderNumber, LocalDate orderDate, String status, BigDecimal total) {
    }

    /**
     * An item aggregated across a client's billable orders (per-hotel item breakdown).
     */
    record ItemTotals(String itemName, String unit, BigDecimal quantity, BigDecimal total) {
    }

    /**
     * Dashboard counts: orders placed on {@code date}, orders currently in progress
     * (RECEIVED or PROCESSING), and orders ready for delivery (DONE).
     */
    StatusCounts statusCounts(LocalDate date);

    /**
     * Sum of every billable order's total with an order date in {@code [from, to]} (all clients).
     */
    BigDecimal billableRevenue(LocalDate from, LocalDate to);

    /**
     * Per-client billable totals over {@code [from, to]} (all clients with at least one order),
     * for the daily and monthly reports. Unordered — the caller sorts/labels.
     */
    List<ClientTotals> billableByClient(LocalDate from, LocalDate to);

    /**
     * Every billable order for one client over {@code [from, to]}, oldest first.
     */
    List<OrderRow> billableOrders(Long clientId, LocalDate from, LocalDate to);

    /**
     * Items aggregated (summed quantity + total) across one client's billable orders over
     * {@code [from, to]}, with item names already resolved. Unordered.
     */
    List<ItemTotals> billableItems(Long clientId, LocalDate from, LocalDate to);
}
