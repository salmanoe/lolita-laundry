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
    // Company letterhead + bank details, frozen when the billing is ISSUED so a later
    // company-profile change never rewrites an issued/paid document. Null while DRAFT — a DRAFT
    // renders from the live profile instead (see MonthlyBillingService).
    private String companyName;
    private String companyAddress;
    private String companyPhone;
    private String bankBeneficiary;
    private String bankName;
    private String bankAccount;
    private String bankHolder;

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

    /**
     * Advances the status by exactly one step ({@code DRAFT → ISSUED → PAID}).
     */
    public void advanceStatus(BillingStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalArgumentException(
                    "Cannot change billing status from %s to %s".formatted(status, target));
        }
        // KI-11: a credit rolled forward off a frozen bill (an edit that shrank or emptied an order
        // already on an ISSUED/PAID document) can leave a DRAFT with a net-negative grand total —
        // money owed back to the client. That draft is a valid running ledger (it nets to positive
        // as the period's later orders join it), but it must never become a client-facing INVOICE.
        // Block the ISSUED transition while the total is negative; staff wait for offsetting orders
        // or settle the credit out of band.
        if (target == BillingStatus.ISSUED && total != null && total.signum() < 0) {
            throw new IllegalArgumentException(
                    "Tagihan ini bersaldo negatif (kredit untuk klien) dan belum bisa diterbitkan. "
                            + "Tunggu order berikutnya pada periode ini atau selesaikan kredit secara manual.");
        }
        this.status = target;
    }

    /**
     * Records the storage key of the rendered PDF.
     */
    public void attachPdf(String storageKey) {
        this.pdfUrl = storageKey;
    }

    /**
     * Freezes the company letterhead + bank details onto this billing. Called when the billing is
     * ISSUED (so the issued/paid document is self-contained and immune to later profile changes),
     * and when reconstituting a persisted billing.
     */
    public void captureCompany(String companyName, String companyAddress, String companyPhone,
                               String bankBeneficiary, String bankName, String bankAccount, String bankHolder) {
        this.companyName = companyName;
        this.companyAddress = companyAddress;
        this.companyPhone = companyPhone;
        this.bankBeneficiary = bankBeneficiary;
        this.bankName = bankName;
        this.bankAccount = bankAccount;
        this.bankHolder = bankHolder;
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

    /**
     * Removes the order's line (e.g. it was canceled) and recomputes the total.
     */
    public void removeLine(Long orderId) {
        lines.removeIf(l -> l.orderId().equals(orderId));
        recomputeTotal();
    }

    /**
     * True when no orders remain on the billing (the row should be deleted).
     */
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