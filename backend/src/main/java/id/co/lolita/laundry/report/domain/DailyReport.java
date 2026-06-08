package id.co.lolita.laundry.report.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Per-client billable totals for a single day, plus the day's grand total.
 */
public record DailyReport(
        LocalDate date,
        List<ClientLine> clients,
        BigDecimal grandTotal) {
}
