package id.co.lolita.laundry.report.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Report's view of order aggregates. The adapter delegates to the order module's
 * {@code OrderReportQuery} (named interface {@code order::api}); these records are report's own
 * snapshots so the order module's types never enter report's domain/application.
 */
public interface OrderReportGateway {

    record StatusCounts(long ordersOnDate, long inProgress, long readyForDelivery) {
    }

    record ClientTotals(Long clientId, long orderCount, BigDecimal total) {
    }

    record OrderRow(Long orderId, String orderNumber, LocalDate orderDate, String status, BigDecimal total) {
    }

    record ItemTotals(String itemName, String unit, BigDecimal quantity, BigDecimal total) {
    }

    StatusCounts statusCounts(LocalDate date);

    BigDecimal billableRevenue(LocalDate from, LocalDate to);

    List<ClientTotals> billableByClient(LocalDate from, LocalDate to);

    List<OrderRow> billableOrders(Long clientId, LocalDate from, LocalDate to);

    List<ItemTotals> billableItems(Long clientId, LocalDate from, LocalDate to);
}
