package id.co.lolita.laundry.report.application;

import id.co.lolita.laundry.report.domain.ClientLine;
import id.co.lolita.laundry.report.domain.DailyReport;
import id.co.lolita.laundry.report.domain.DashboardSummary;
import id.co.lolita.laundry.report.domain.HotelReport;
import id.co.lolita.laundry.report.domain.MonthlyReport;
import id.co.lolita.laundry.report.domain.port.in.GetDashboardUseCase;
import id.co.lolita.laundry.report.domain.port.in.GetReportsUseCase;
import id.co.lolita.laundry.report.domain.port.out.ClientLookupGateway;
import id.co.lolita.laundry.report.domain.port.out.OrderReportGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/**
 * Phase 4 dashboard + reports. Reads order aggregates through {@link OrderReportGateway} and
 * labels rows with client names via {@link ClientLookupGateway}; all money is "billable orders
 * by order date" (multiplier-inclusive), not coupled to billing status.
 */
@Service
@RequiredArgsConstructor
class ReportService implements GetDashboardUseCase, GetReportsUseCase {

    private final OrderReportGateway orders;
    private final ClientLookupGateway clients;

    @Override
    public DashboardSummary dashboard() {
        LocalDate today = LocalDate.now();
        var counts = orders.statusCounts(today);
        YearMonth month = YearMonth.from(today);
        BigDecimal revenue = orders.billableRevenue(month.atDay(1), month.atEndOfMonth());
        return new DashboardSummary(counts.ordersOnDate(), counts.inProgress(),
                counts.readyForDelivery(), revenue);
    }

    @Override
    public DailyReport daily(LocalDate date) {
        List<ClientLine> lines = clientLines(orders.billableByClient(date, date));
        return new DailyReport(date, lines, grandTotal(lines));
    }

    @Override
    public MonthlyReport monthly(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<ClientLine> lines = clientLines(orders.billableByClient(ym.atDay(1), ym.atEndOfMonth()));
        return new MonthlyReport(year, month, lines, grandTotal(lines));
    }

    @Override
    public HotelReport hotel(Long clientId, LocalDate from, LocalDate to) {
        var client = clients.findById(clientId);
        String name = client.map(ClientLookupGateway.ClientInfo::name).orElse("#" + clientId);
        String code = client.map(ClientLookupGateway.ClientInfo::clientCode).orElse(null);

        var orderLines = orders.billableOrders(clientId, from, to).stream()
                .map(o -> new HotelReport.OrderLine(o.orderId(), o.orderNumber(), o.orderDate(),
                        o.status(), o.total()))
                .toList();
        var itemLines = orders.billableItems(clientId, from, to).stream()
                .map(i -> new HotelReport.ItemLine(i.itemName(), i.unit(), i.quantity(), i.total()))
                .sorted(Comparator.comparing(HotelReport.ItemLine::itemName))
                .toList();
        BigDecimal grandTotal = orderLines.stream()
                .map(HotelReport.OrderLine::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new HotelReport(clientId, name, code, from, to, orderLines, itemLines, grandTotal);
    }

    /**
     * Resolves each client's name/code and sorts the rows by client name.
     */
    private List<ClientLine> clientLines(List<OrderReportGateway.ClientTotals> totals) {
        return totals.stream().map(t -> {
            var client = clients.findById(t.clientId());
            String name = client.map(ClientLookupGateway.ClientInfo::name).orElse("#" + t.clientId());
            String code = client.map(ClientLookupGateway.ClientInfo::clientCode).orElse(null);
            return new ClientLine(t.clientId(), name, code, t.orderCount(), t.total());
        }).sorted(Comparator.comparing(ClientLine::clientName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private static BigDecimal grandTotal(List<ClientLine> lines) {
        return lines.stream().map(ClientLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}