package id.co.lolita.laundry.billing.domain;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * A client's billing cycle — the rule that maps an order date onto a billing period, and a
 * billing period onto its calendar date range.
 *
 * <p>The default ({@code cutoffDay == null}) is the plain calendar month: 1st through the last
 * day. A client with a contractual cut-off (e.g. PBS-style "invoice every 25th") gets a period
 * running from the day <em>after</em> the cut-off in the previous month through the cut-off in
 * the period's own month — so cut-off 25 yields {@code 26 Jul – 25 Aug}.
 *
 * <p><strong>A cycle still produces exactly one period per month.</strong> The period keeps its
 * {@link YearMonth} identity, labelled by the month it <em>ends</em> in ({@code 26 Jul – 25 Aug}
 * is {@code 2026-08}). That is what lets the billing number, the
 * {@code UNIQUE(client, department, period_year, period_month)} constraint and the
 * roll-forward-to-the-next-period logic stay exactly as they are.
 *
 * <p>The cut-off is capped at 28 by a DB CHECK so {@link #endOf} never has to clamp to a short
 * February.
 */
public record BillingCycle(Integer cutoffDay) {

    /** The plain calendar month — every client's default. */
    public static final BillingCycle CALENDAR = new BillingCycle(null);

    public BillingCycle {
        if (cutoffDay != null && (cutoffDay < 1 || cutoffDay > 28)) {
            throw new IllegalArgumentException("Billing cut-off day must be between 1 and 28: " + cutoffDay);
        }
    }

    /**
     * Builds a cycle from a nullable cut-off day (null → calendar month).
     */
    public static BillingCycle of(Integer cutoffDay) {
        return cutoffDay == null ? CALENDAR : new BillingCycle(cutoffDay);
    }

    /** True when this is the plain calendar month. */
    public boolean isCalendar() {
        return cutoffDay == null;
    }

    /**
     * The billing period an order date falls into. On or before the cut-off the date belongs to
     * its own month; after the cut-off it belongs to the next month's period.
     */
    public YearMonth periodOf(LocalDate date) {
        var ym = YearMonth.from(date);
        if (cutoffDay == null || date.getDayOfMonth() <= cutoffDay) {
            return ym;
        }
        return ym.plusMonths(1);
    }

    /** The first date covered by a period. */
    public LocalDate startOf(YearMonth period) {
        return cutoffDay == null ? period.atDay(1) : period.minusMonths(1).atDay(cutoffDay).plusDays(1);
    }

    /** The last date covered by a period. */
    public LocalDate endOf(YearMonth period) {
        return cutoffDay == null ? period.atEndOfMonth() : period.atDay(cutoffDay);
    }
}
