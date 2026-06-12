package id.co.lolita.laundry.report.domain.port.in;

import id.co.lolita.laundry.report.domain.DashboardAnalytics;
import id.co.lolita.laundry.report.domain.DashboardSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Inbound port: build the dashboards. {@link #dashboard()} is the operational summary
 * (FINANCE_STAFF/SUPER_ADMIN); {@link #analytics(LocalDate, LocalDate)} is the SUPER_ADMIN-only
 * business-analytics view; {@link #financeTrend(int)} is the FINANCE_STAFF dashboard's monthly
 * revenue + order-count series.
 */
public interface GetDashboardUseCase {

    DashboardSummary dashboard();

    DashboardAnalytics analytics(LocalDate from, LocalDate to);

    /**
     * Revenue + order count for each of the last {@code months} months (oldest first, includes the current partial month).
     */
    List<MonthlyTotals> financeTrend(int months);

    record MonthlyTotals(YearMonth month, BigDecimal revenue, long orderCount) {
    }
}