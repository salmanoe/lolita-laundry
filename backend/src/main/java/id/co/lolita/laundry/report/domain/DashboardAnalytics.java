package id.co.lolita.laundry.report.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Business-analytics view for the owner dashboard over a date range. All money is "billable
 * orders by order date" (multiplier-inclusive), the same basis as the Phase 4 reports, so the
 * totals reconcile with the Reports page.
 *
 * @param from          inclusive range start
 * @param to            inclusive range end
 * @param totalRevenue  sum of every billable order's total in the range
 * @param totalOrders   count of billable (non-canceled) orders in the range
 * @param avgOrderValue totalRevenue / totalOrders (0 when there are no orders)
 * @param bestMonth     the highest-revenue month in the range (null when the range has no orders)
 * @param hotels        per-client totals, ranked by revenue desc; doubles as the chart legend/color order
 * @param months        chronological per-month points (revenue + per-hotel slices)
 */
public record DashboardAnalytics(
        LocalDate from,
        LocalDate to,
        BigDecimal totalRevenue,
        long totalOrders,
        BigDecimal avgOrderValue,
        BestMonth bestMonth,
        List<HotelTotal> hotels,
        List<MonthPoint> months) {

    public record BestMonth(YearMonth month, BigDecimal revenue) {
    }

    public record HotelTotal(Long clientId, String name, String code, long orderCount, BigDecimal revenue) {
    }

    public record MonthPoint(YearMonth month, BigDecimal revenue, boolean partial, List<HotelSlice> perHotel) {
    }

    public record HotelSlice(Long clientId, BigDecimal revenue) {
    }
}