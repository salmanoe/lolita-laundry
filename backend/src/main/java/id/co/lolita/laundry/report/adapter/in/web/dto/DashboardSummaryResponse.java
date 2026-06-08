package id.co.lolita.laundry.report.adapter.in.web.dto;

import id.co.lolita.laundry.report.domain.DashboardSummary;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long ordersToday,
        long inProgress,
        long readyForDelivery,
        BigDecimal revenueThisMonth) {

    public static DashboardSummaryResponse from(DashboardSummary s) {
        return new DashboardSummaryResponse(s.ordersToday(), s.inProgress(),
                s.readyForDelivery(), s.revenueThisMonth());
    }
}