package id.co.lolita.laundry.report.domain;

import java.math.BigDecimal;

/**
 * At-a-glance operational state for the owner/staff dashboard.
 *
 * @param ordersToday      orders placed today (by order date)
 * @param inProgress       orders currently RECEIVED or PROCESSING
 * @param readyForDelivery orders marked DONE, awaiting delivery
 * @param revenueThisMonth sum of every billable order's total in the current calendar month
 */
public record DashboardSummary(
        long ordersToday,
        long inProgress,
        long readyForDelivery,
        BigDecimal revenueThisMonth) {
}
