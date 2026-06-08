package id.co.lolita.laundry.report.adapter.in.web.dto;

import id.co.lolita.laundry.report.domain.DashboardAnalytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Owner business-analytics dashboard payload. {@code YearMonth} values are serialized as
 * {@code "YYYY-MM"} strings for the frontend.
 */
public record DashboardAnalyticsResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalRevenue,
        long totalOrders,
        BigDecimal avgOrderValue,
        BestMonth bestMonth,
        List<HotelTotal> hotels,
        List<MonthPoint> months) {

    public record BestMonth(String month, BigDecimal revenue) {
    }

    public record HotelTotal(Long clientId, String name, String code, long orderCount, BigDecimal revenue) {
    }

    public record MonthPoint(String month, BigDecimal revenue, boolean partial, List<HotelSlice> perHotel) {
    }

    public record HotelSlice(Long clientId, BigDecimal revenue) {
    }

    public static DashboardAnalyticsResponse from(DashboardAnalytics a) {
        BestMonth best = a.bestMonth() == null
                ? null
                : new BestMonth(a.bestMonth().month().toString(), a.bestMonth().revenue());

        List<HotelTotal> hotels = a.hotels().stream()
                .map(h -> new HotelTotal(h.clientId(), h.name(), h.code(), h.orderCount(), h.revenue()))
                .toList();

        List<MonthPoint> months = a.months().stream()
                .map(m -> new MonthPoint(m.month().toString(), m.revenue(), m.partial(),
                        m.perHotel().stream()
                                .map(s -> new HotelSlice(s.clientId(), s.revenue()))
                                .toList()))
                .toList();

        return new DashboardAnalyticsResponse(a.from(), a.to(), a.totalRevenue(), a.totalOrders(),
                a.avgOrderValue(), best, hotels, months);
    }
}