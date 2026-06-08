package id.co.lolita.laundry.order.application;

import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderLineItem;
import id.co.lolita.laundry.order.domain.OrderStatus;
import id.co.lolita.laundry.order.domain.port.in.OrderReportQuery;
import id.co.lolita.laundry.order.domain.port.out.CatalogGateway;
import id.co.lolita.laundry.order.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only order aggregates for the {@code report} module (Phase 4 dashboard/reports).
 * Aggregation happens in Java over the billable order set — at this scale (a handful of clients,
 * a few hundred orders a month) that is trivial. Lets us reuse {@link Order#total()} and the
 * catalogue item-name resolution rather than re-deriving money in SQL.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class OrderReportService implements OrderReportQuery {

    private final OrderRepository orderRepository;
    private final CatalogGateway catalogGateway;

    @Override
    public StatusCounts statusCounts(LocalDate date) {
        long ordersOnDate = orderRepository.countByOrderDate(date);
        long inProgress = orderRepository.countByStatuses(List.of(OrderStatus.RECEIVED, OrderStatus.PROCESSING));
        long readyForDelivery = orderRepository.countByStatuses(List.of(OrderStatus.DONE));
        return new StatusCounts(ordersOnDate, inProgress, readyForDelivery);
    }

    @Override
    public BigDecimal billableRevenue(LocalDate from, LocalDate to) {
        return orderRepository.findBillableInPeriod(from, to).stream()
                .map(Order::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<ClientTotals> billableByClient(LocalDate from, LocalDate to) {
        Map<Long, long[]> counts = new LinkedHashMap<>();
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        for (Order o : orderRepository.findBillableInPeriod(from, to)) {
            counts.computeIfAbsent(o.getClientId(), _ -> new long[1])[0]++;
            totals.merge(o.getClientId(), o.total(), BigDecimal::add);
        }
        return counts.entrySet().stream()
                .map(e -> new ClientTotals(e.getKey(), e.getValue()[0], totals.get(e.getKey())))
                .toList();
    }

    @Override
    public List<OrderRow> billableOrders(Long clientId, LocalDate from, LocalDate to) {
        return orderRepository.findBillableByClientAndPeriod(clientId, from, to).stream()
                .map(o -> new OrderRow(o.getId(), o.getOrderNumber(), o.getOrderDate(),
                        o.getStatus().name(), o.total()))
                .toList();
    }

    @Override
    public List<ItemTotals> billableItems(Long clientId, LocalDate from, LocalDate to) {
        // Aggregate quantity + subtotal per item across the client's billable orders.
        Map<Long, BigDecimal> qty = new LinkedHashMap<>();
        Map<Long, BigDecimal> sub = new LinkedHashMap<>();
        for (Order o : orderRepository.findBillableByClientAndPeriod(clientId, from, to)) {
            for (OrderLineItem li : o.getLineItems()) {
                qty.merge(li.itemId(), li.quantity(), BigDecimal::add);
                sub.merge(li.itemId(), li.subtotal(), BigDecimal::add);
            }
        }
        return qty.entrySet().stream().map(e -> {
            Long itemId = e.getKey();
            var item = catalogGateway.findActiveById(itemId);
            String name = item.map(CatalogGateway.CatalogItem::name).orElse("#" + itemId);
            String unit = item.map(CatalogGateway.CatalogItem::unitName).orElse(null);
            return new ItemTotals(name, unit, e.getValue(), sub.get(itemId));
        }).toList();
    }
}
