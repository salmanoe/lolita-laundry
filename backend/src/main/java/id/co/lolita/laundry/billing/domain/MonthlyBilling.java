package id.co.lolita.laundry.billing.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Monthly Billing — the actual payment document sent to a client once a month. Aggregates
 * every delivered order for the period at order level (one {@link MonthlyBillingLine} per
 * order) into a grand total.
 *
 * <p>For COMBINED clients there is one billing per month ({@code departmentId == null}). For
 * PER_DEPARTMENT clients (e.g. PBS) there is one billing per department per month. The
 * lifecycle is {@code DRAFT → ISSUED → PAID}; only a DRAFT may be regenerated.
 */
@Getter
public class MonthlyBilling {

    private final Long id;
    private final String billingNumber;
    private final Long clientId;
    private final Long departmentId;      // nullable — set only for PER_DEPARTMENT clients
    private final int periodYear;
    private final int periodMonth;
    private final LocalDate invoiceDate;
    private final BigDecimal total;
    private BillingStatus status;
    private String pdfUrl;                // storage object key — nullable until the PDF is rendered
    private final String notes;
    private final Instant createdAt;
    private final List<MonthlyBillingLine> lines = new ArrayList<>();

    public MonthlyBilling(Long id, String billingNumber, Long clientId, Long departmentId, int periodYear,
                          int periodMonth, LocalDate invoiceDate, BigDecimal total, BillingStatus status,
                          String pdfUrl, String notes, Instant createdAt, List<MonthlyBillingLine> lines) {
        this.id = id;
        this.billingNumber = billingNumber;
        this.clientId = clientId;
        this.departmentId = departmentId;
        this.periodYear = periodYear;
        this.periodMonth = periodMonth;
        this.invoiceDate = invoiceDate;
        this.total = total;
        this.status = status;
        this.pdfUrl = pdfUrl;
        this.notes = notes;
        this.createdAt = createdAt;
        if (lines != null) {
            this.lines.addAll(lines);
        }
    }

    /**
     * Generates a fresh DRAFT billing from the period's delivered-order lines. The grand total
     * is the sum of line subtotals. Requires at least one line — empty periods are not billed.
     */
    public static MonthlyBilling generate(String billingNumber, Long clientId, Long departmentId, int periodYear,
                                          int periodMonth, LocalDate invoiceDate, List<MonthlyBillingLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Cannot generate a billing with no delivered orders");
        }
        var total = lines.stream()
                .map(MonthlyBillingLine::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MonthlyBilling(null, billingNumber, clientId, departmentId, periodYear, periodMonth,
                invoiceDate, total, BillingStatus.DRAFT, null, null, Instant.now(), lines);
    }

    /** Advances the status by exactly one step ({@code DRAFT → ISSUED → PAID}). */
    public void advanceStatus(BillingStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalArgumentException(
                    "Cannot change billing status from %s to %s".formatted(status, target));
        }
        this.status = target;
    }

    /** Records the storage key of the rendered PDF. */
    public void attachPdf(String storageKey) {
        this.pdfUrl = storageKey;
    }

    public List<MonthlyBillingLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}