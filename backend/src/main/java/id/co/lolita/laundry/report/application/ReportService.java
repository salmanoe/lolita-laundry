package id.co.lolita.laundry.report.application;

import id.co.lolita.laundry.report.domain.ClientLine;
import id.co.lolita.laundry.report.domain.DailyReport;
import id.co.lolita.laundry.report.domain.DashboardAnalytics;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public DashboardAnalytics analytics(LocalDate from, LocalDate to) {
        YearMonth currentMonth = YearMonth.from(LocalDate.now());

        List<DashboardAnalytics.MonthPoint> months = new ArrayList<>();
        Map<Long, BigDecimal> revenueByClient = new LinkedHashMap<>();
        Map<Long, Long> orderCountByClient = new LinkedHashMap<>();

        // Walk each month in the range; clamp to [from, to] so a mid-month range edge counts
        // only the orders inside it. One billableByClient call per month (≤12 for a year).
        for (YearMonth ym = YearMonth.from(from); !ym.isAfter(YearMonth.from(to)); ym = ym.plusMonths(1)) {
            LocalDate monthStart = ym.atDay(1).isBefore(from) ? from : ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth().isAfter(to) ? to : ym.atEndOfMonth();

            BigDecimal monthRevenue = BigDecimal.ZERO;
            List<DashboardAnalytics.HotelSlice> slices = new ArrayList<>();
            for (var t : orders.billableByClient(monthStart, monthEnd)) {
                monthRevenue = monthRevenue.add(t.total());
                slices.add(new DashboardAnalytics.HotelSlice(t.clientId(), t.total()));
                revenueByClient.merge(t.clientId(), t.total(), BigDecimal::add);
                orderCountByClient.merge(t.clientId(), t.orderCount(), Long::sum);
            }
            months.add(new DashboardAnalytics.MonthPoint(ym, monthRevenue, ym.equals(currentMonth), slices));
        }

        // Ranked per-hotel totals (revenue desc, name as tiebreak) — also the chart legend/color order.
        List<DashboardAnalytics.HotelTotal> hotels = revenueByClient.entrySet().stream()
                .map(e -> {
                    Long clientId = e.getKey();
                    var client = clients.findById(clientId);
                    String name = client.map(ClientLookupGateway.ClientInfo::name).orElse("#" + clientId);
                    String code = client.map(ClientLookupGateway.ClientInfo::clientCode).orElse(null);
                    return new DashboardAnalytics.HotelTotal(clientId, name, code,
                            orderCountByClient.getOrDefault(clientId, 0L), e.getValue());
                })
                .sorted(Comparator.comparing(DashboardAnalytics.HotelTotal::revenue).reversed()
                        .thenComparing(DashboardAnalytics.HotelTotal::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        BigDecimal totalRevenue = Objects.requireNonNullElse(orders.billableRevenue(from, to), BigDecimal.ZERO);
        long totalOrders = orderCountByClient.values().stream().mapToLong(Long::longValue).sum();
        BigDecimal avgOrderValue = totalOrders == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, RoundingMode.HALF_UP);

        DashboardAnalytics.BestMonth bestMonth = months.stream()
                .filter(m -> m.revenue().signum() > 0)
                .max(Comparator.comparing(DashboardAnalytics.MonthPoint::revenue))
                .map(m -> new DashboardAnalytics.BestMonth(m.month(), m.revenue()))
                .orElse(null);

        return new DashboardAnalytics(from, to, totalRevenue, totalOrders, avgOrderValue,
                bestMonth, hotels, months);
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