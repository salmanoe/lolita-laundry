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
    private final String departmentName;  // denormalized for the invoice PDF; null for COMBINED
    private final int periodYear;
    private final int periodMonth;
    private final LocalDate invoiceDate;
    private BigDecimal total;             // recomputed as lines are upserted/removed
    private BillingStatus status;
    private String pdfUrl;                // storage object key — nullable until the PDF is rendered
    private final String notes;
    private final Instant createdAt;
    private final List<MonthlyBillingLine> lines = new ArrayList<>();

    public MonthlyBilling(Long id, String billingNumber, Long clientId, Long departmentId, String departmentName,
                          int periodYear, int periodMonth, LocalDate invoiceDate, BigDecimal total,
                          BillingStatus status, String pdfUrl, String notes, Instant createdAt,
                          List<MonthlyBillingLine> lines) {
        this.id = id;
        this.billingNumber = billingNumber;
        this.clientId = clientId;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
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
    public static MonthlyBilling generate(String billingNumber, Long clientId, Long departmentId,
                                          String departmentName, int periodYear, int periodMonth,
                                          LocalDate invoiceDate, List<MonthlyBillingLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Cannot generate a billing with no delivered orders");
        }
        var total = lines.stream()
                .map(MonthlyBillingLine::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MonthlyBilling(null, billingNumber, clientId, departmentId, departmentName, periodYear,
                periodMonth, invoiceDate, total, BillingStatus.DRAFT, null, null, Instant.now(), lines);
    }

    /**
     * Starts a fresh empty DRAFT billing for a period — the auto-build sync creates one on the
     * first order of the month, then {@link #upsertLine} accumulates orders into it.
     */
    public static MonthlyBilling startNew(String billingNumber, Long clientId, Long departmentId,
                                          String departmentName, int periodYear, int periodMonth,
                                          LocalDate invoiceDate) {
        return new MonthlyBilling(null, billingNumber, clientId, departmentId, departmentName, periodYear,
                periodMonth, invoiceDate, BigDecimal.ZERO, BillingStatus.DRAFT, null, null, Instant.now(),
                new ArrayList<>());
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

    /**
     * Adds the order's line, or replaces it if the order is already on this billing (its amount
     * may have changed via an edit), then recomputes the total. Used by the auto-build sync.
     */
    public void upsertLine(MonthlyBillingLine line) {
        lines.removeIf(l -> l.orderId().equals(line.orderId()));
        lines.add(line);
        recomputeTotal();
    }

    /** Removes the order's line (e.g. it was cancelled) and recomputes the total. */
    public void removeLine(Long orderId) {
        lines.removeIf(l -> l.orderId().equals(orderId));
        recomputeTotal();
    }

    /** True when no orders remain on the billing (the row should be deleted). */
    public boolean isEmpty() {
        return lines.isEmpty();
    }

    private void recomputeTotal() {
        this.total = lines.stream().map(MonthlyBillingLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<MonthlyBillingLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}