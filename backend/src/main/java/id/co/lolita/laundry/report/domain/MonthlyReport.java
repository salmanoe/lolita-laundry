package id.co.lolita.laundry.report.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Per-client billable totals for a calendar month, plus the month's grand total.
 */
public record MonthlyReport(
        int year,
        int month,
        List<ClientLine> clients,
        BigDecimal grandTotal) {
}
