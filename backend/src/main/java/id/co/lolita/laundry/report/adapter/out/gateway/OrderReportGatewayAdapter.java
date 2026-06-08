package id.co.lolita.laundry.report.adapter.out.gateway;

import id.co.lolita.laundry.order.domain.port.in.OrderReportQuery;
import id.co.lolita.laundry.report.domain.port.out.OrderReportGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Bridges report's {@link OrderReportGateway} to the order module's {@link OrderReportQuery}
 * (named interface {@code order::api}), mapping order snapshots to report's own records.
 */
@Component
@RequiredArgsConstructor
class OrderReportGatewayAdapter implements OrderReportGateway {

    private final OrderReportQuery orders;

    @Override
    public StatusCounts statusCounts(LocalDate date) {
        var c = orders.statusCounts(date);
        return new StatusCounts(c.ordersOnDate(), c.inProgress(), c.readyForDelivery());
    }

    @Override
    public BigDecimal billableRevenue(LocalDate from, LocalDate to) {
        return orders.billableRevenue(from, to);
    }

    @Override
    public List<ClientTotals> billableByClient(LocalDate from, LocalDate to) {
        return orders.billableByClient(from, to).stream()
                .map(t -> new ClientTotals(t.clientId(), t.orderCount(), t.total()))
                .toList();
    }

    @Override
    public List<OrderRow> billableOrders(Long clientId, LocalDate from, LocalDate to) {
        return orders.billableOrders(clientId, from, to).stream()
                .map(o -> new OrderRow(o.orderId(), o.orderNumber(), o.orderDate(), o.status(), o.total()))
                .toList();
    }

    @Override
    public List<ItemTotals> billableItems(Long clientId, LocalDate from, LocalDate to) {
        return orders.billableItems(clientId, from, to).stream()
                .map(i -> new ItemTotals(i.itemName(), i.unit(), i.quantity(), i.total()))
                .toList();
    }
}
