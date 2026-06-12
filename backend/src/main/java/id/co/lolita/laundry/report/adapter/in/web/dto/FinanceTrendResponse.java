package id.co.lolita.laundry.report.adapter.in.web.dto;

import id.co.lolita.laundry.report.domain.port.in.GetDashboardUseCase.MonthlyTotals;

import java.math.BigDecimal;

/**
 * One month on the FINANCE_STAFF dashboard trend: revenue + order count. {@code month} is "YYYY-MM".
 */
public record FinanceTrendResponse(String month, BigDecimal revenue, long orderCount) {
    public static FinanceTrendResponse from(MonthlyTotals m) {
        return new FinanceTrendResponse(m.month().toString(), m.revenue(), m.orderCount());
    }
}
