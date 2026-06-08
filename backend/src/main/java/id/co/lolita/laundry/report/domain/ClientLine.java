package id.co.lolita.laundry.report.domain;

import java.math.BigDecimal;

/**
 * One client's billable totals over a report period (daily / monthly report row).
 */
public record ClientLine(
        Long clientId,
        String clientName,
        String clientCode,
        long orderCount,
        BigDecimal total) {
}