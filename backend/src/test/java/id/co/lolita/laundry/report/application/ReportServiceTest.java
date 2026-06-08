package id.co.lolita.laundry.report.application;

import id.co.lolita.laundry.report.domain.DailyReport;
import id.co.lolita.laundry.report.domain.DashboardAnalytics;
import id.co.lolita.laundry.report.domain.DashboardSummary;
import id.co.lolita.laundry.report.domain.HotelReport;
import id.co.lolita.laundry.report.domain.MonthlyReport;
import id.co.lolita.laundry.report.domain.port.out.ClientLookupGateway;
import id.co.lolita.laundry.report.domain.port.out.ClientLookupGateway.ClientInfo;
import id.co.lolita.laundry.report.domain.port.out.OrderReportGateway;
import id.co.lolita.laundry.report.domain.port.out.OrderReportGateway.ClientTotals;
import id.co.lolita.laundry.report.domain.port.out.OrderReportGateway.ItemTotals;
import id.co.lolita.laundry.report.domain.port.out.OrderReportGateway.OrderRow;
import id.co.lolita.laundry.report.domain.port.out.OrderReportGateway.StatusCounts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    OrderReportGateway orders;
    @Mock
    ClientLookupGateway clients;
    @InjectMocks
    ReportService service;

    @Test
    void dashboardMapsCountsAndCurrentMonthRevenue() {
        when(orders.statusCounts(any())).thenReturn(new StatusCounts(3, 5, 2));
        when(orders.billableRevenue(any(), any())).thenReturn(new BigDecimal("1500000"));

        DashboardSummary s = service.dashboard();

        assertThat(s.ordersToday()).isEqualTo(3);
        assertThat(s.inProgress()).isEqualTo(5);
        assertThat(s.readyForDelivery()).isEqualTo(2);
        assertThat(s.revenueThisMonth()).isEqualByComparingTo("1500000");
    }

    @Test
    void dailyResolvesClientNamesSortsAndSumsGrandTotal() {
        LocalDate date = LocalDate.of(2026, 6, 8);
        when(orders.billableByClient(date, date)).thenReturn(List.of(
                new ClientTotals(2L, 1, new BigDecimal("200000")),   // "Zeta" — should sort last
                new ClientTotals(1L, 3, new BigDecimal("300000"))    // "Alpha" — should sort first
        ));
        when(clients.findById(1L)).thenReturn(Optional.of(new ClientInfo(1L, "Alpha", "ALP")));
        when(clients.findById(2L)).thenReturn(Optional.of(new ClientInfo(2L, "Zeta", "ZET")));

        DailyReport r = service.daily(date);

        assertThat(r.date()).isEqualTo(date);
        assertThat(r.clients()).extracting("clientName").containsExactly("Alpha", "Zeta");
        assertThat(r.clients().getFirst().orderCount()).isEqualTo(3);
        assertThat(r.grandTotal()).isEqualByComparingTo("500000");
    }

    @Test
    void monthlyAggregatesPerClientWithGrandTotal() {
        when(orders.billableByClient(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(List.of(new ClientTotals(1L, 4, new BigDecimal("900000"))));
        when(clients.findById(1L)).thenReturn(Optional.of(new ClientInfo(1L, "PBS", "PBS")));

        MonthlyReport r = service.monthly(2026, 6);

        assertThat(r.year()).isEqualTo(2026);
        assertThat(r.month()).isEqualTo(6);
        assertThat(r.clients()).hasSize(1);
        assertThat(r.grandTotal()).isEqualByComparingTo("900000");
    }

    @Test
    void missingClientFallsBackToIdLabel() {
        LocalDate date = LocalDate.of(2026, 6, 8);
        when(orders.billableByClient(date, date))
                .thenReturn(List.of(new ClientTotals(99L, 1, new BigDecimal("50000"))));
        when(clients.findById(99L)).thenReturn(Optional.empty());

        DailyReport r = service.daily(date);

        assertThat(r.clients().getFirst().clientName()).isEqualTo("#99");
    }

    @Test
    void analyticsAggregatesRangeRanksHotelsAndFlagsPartialMonth() {
        // 3 months ending at the current month; the last month is partial.
        YearMonth m0 = YearMonth.now().minusMonths(2);
        YearMonth m1 = YearMonth.now().minusMonths(1);
        YearMonth m2 = YearMonth.now();
        LocalDate from = m0.atDay(1);
        LocalDate to = LocalDate.now(); // YTD-style: clamps the current month to today

        when(orders.billableByClient(m0.atDay(1), m0.atEndOfMonth())).thenReturn(List.of(
                new ClientTotals(1L, 2, new BigDecimal("200000")),
                new ClientTotals(2L, 1, new BigDecimal("100000"))));
        when(orders.billableByClient(m1.atDay(1), m1.atEndOfMonth())).thenReturn(List.of(
                new ClientTotals(1L, 1, new BigDecimal("300000"))));
        when(orders.billableByClient(m2.atDay(1), to)).thenReturn(List.of(
                new ClientTotals(2L, 1, new BigDecimal("500000"))));
        when(orders.billableRevenue(from, to)).thenReturn(new BigDecimal("1100000"));
        when(clients.findById(1L)).thenReturn(Optional.of(new ClientInfo(1L, "Alpha", "ALP")));
        when(clients.findById(2L)).thenReturn(Optional.of(new ClientInfo(2L, "Beta", "BET")));

        DashboardAnalytics a = service.analytics(from, to);

        assertThat(a.totalRevenue()).isEqualByComparingTo("1100000");
        assertThat(a.totalOrders()).isEqualTo(5);
        assertThat(a.avgOrderValue()).isEqualByComparingTo("220000"); // 1,100,000 / 5

        // Beta = 100k+500k = 600k (ranks first); Alpha = 200k+300k = 500k.
        assertThat(a.hotels()).extracting(DashboardAnalytics.HotelTotal::clientId).containsExactly(2L, 1L);
        assertThat(a.hotels().getFirst().revenue()).isEqualByComparingTo("600000");
        assertThat(a.hotels().getFirst().orderCount()).isEqualTo(2);

        assertThat(a.bestMonth().month()).isEqualTo(m2);
        assertThat(a.bestMonth().revenue()).isEqualByComparingTo("500000");

        assertThat(a.months()).hasSize(3);
        assertThat(a.months().getFirst().month()).isEqualTo(m0);
        assertThat(a.months().get(0).partial()).isFalse();
        assertThat(a.months().get(0).revenue()).isEqualByComparingTo("300000"); // 200k + 100k
        assertThat(a.months().get(2).partial()).isTrue();
    }

    @Test
    void analyticsEmptyRangeHasNoBestMonthAndZeroAvg() {
        YearMonth past = YearMonth.now().minusMonths(1);
        LocalDate from = past.atDay(1);
        LocalDate to = past.atEndOfMonth();
        when(orders.billableByClient(any(), any())).thenReturn(List.of());
        when(orders.billableRevenue(from, to)).thenReturn(BigDecimal.ZERO);

        DashboardAnalytics a = service.analytics(from, to);

        assertThat(a.totalOrders()).isZero();
        assertThat(a.avgOrderValue()).isEqualByComparingTo("0");
        assertThat(a.bestMonth()).isNull();
        assertThat(a.hotels()).isEmpty();
        assertThat(a.months()).hasSize(1);
        assertThat(a.months().getFirst().revenue()).isEqualByComparingTo("0");
    }

    @Test
    void hotelBuildsOrdersItemsAndGrandTotalFromOrderTotals() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(clients.findById(7L)).thenReturn(Optional.of(new ClientInfo(7L, "Frances", "FRA")));
        when(orders.billableOrders(7L, from, to)).thenReturn(List.of(
                new OrderRow(10L, "FRA-20260601-001", from, "DELIVERED", new BigDecimal("120000")),
                new OrderRow(11L, "FRA-20260602-001", from.plusDays(1), "RECEIVED", new BigDecimal("80000"))
        ));
        when(orders.billableItems(7L, from, to)).thenReturn(List.of(
                new ItemTotals("Towel", "pcs", new BigDecimal("10"), new BigDecimal("50000")),
                new ItemTotals("Apron", "pcs", new BigDecimal("4"), new BigDecimal("20000"))
        ));

        HotelReport r = service.hotel(7L, from, to);

        assertThat(r.clientName()).isEqualTo("Frances");
        assertThat(r.orders()).hasSize(2);
        assertThat(r.items()).extracting("itemName").containsExactly("Apron", "Towel"); // sorted
        assertThat(r.grandTotal()).isEqualByComparingTo("200000");
    }
}
