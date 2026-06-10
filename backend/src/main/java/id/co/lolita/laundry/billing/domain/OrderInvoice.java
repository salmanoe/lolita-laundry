package id.co.lolita.laundry.billing.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The per-order invoice — one per order, for every client. An itemized reference document.
 * It is viewable from the moment the order is RECEIVED: while the order is still open
 * (editable) the invoice is a <em>preview</em> that re-renders to reflect edits (see
 * {@link #refresh}); once the order is DELIVERED it is frozen — the letterhead and totals are
 * never rewritten afterward, so a later company-profile change can't alter a settled invoice.
 * The {@code subtotal} is the order total (the multiplier is already baked into each line).
 * {@code pdfUrl} is the storage key of the rendered PDF, attached once the PDF is generated.
 */
@Getter
public class OrderInvoice {

    private final Long id;
    private final String invoiceNumber;
    private final Long orderId;
    private final Long clientId;
    private LocalDate invoiceDate;
    private BigDecimal subtotal;
    private String pdfUrl;            // storage object key — nullable until the PDF is rendered
    private final Instant createdAt;
    // Company letterhead — re-snapshotted on every preview refresh, then frozen once the order
    // is delivered, so a later company-profile change never rewrites a settled invoice.
    private String companyName;
    private String companyAddress;
    private String companyPhone;

    public OrderInvoice(Long id, String invoiceNumber, Long orderId, Long clientId, LocalDate invoiceDate,
                        BigDecimal subtotal, String pdfUrl, Instant createdAt,
                        String companyName, String companyAddress, String companyPhone) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.orderId = orderId;
        this.clientId = clientId;
        this.invoiceDate = invoiceDate;
        this.subtotal = subtotal;
        this.pdfUrl = pdfUrl;
        this.createdAt = createdAt;
        this.companyName = companyName;
        this.companyAddress = companyAddress;
        this.companyPhone = companyPhone;
    }

    /**
     * Builds a new (unsaved) invoice for an order, snapshotting the company letterhead as it
     * stands now. PDF attached separately.
     */
    public static OrderInvoice create(String invoiceNumber, Long orderId, Long clientId,
                                      LocalDate invoiceDate, BigDecimal subtotal,
                                      String companyName, String companyAddress, String companyPhone) {
        if (subtotal == null || subtotal.signum() < 0) {
            throw new IllegalArgumentException("Invoice subtotal must be zero or positive");
        }
        return new OrderInvoice(null, invoiceNumber, orderId, clientId, invoiceDate, subtotal, null, Instant.now(),
                companyName, companyAddress, companyPhone);
    }

    /**
     * Refreshes a still-open invoice's totals, date and letterhead from the current order/company
     * state. Used while the order is editable (RECEIVED/PROCESSING/DONE) so the preview stays
     * current; never call this once the order is delivered — a settled invoice is immutable.
     */
    public void refresh(LocalDate invoiceDate, BigDecimal subtotal,
                        String companyName, String companyAddress, String companyPhone) {
        if (subtotal == null || subtotal.signum() < 0) {
            throw new IllegalArgumentException("Invoice subtotal must be zero or positive");
        }
        this.invoiceDate = invoiceDate;
        this.subtotal = subtotal;
        this.companyName = companyName;
        this.companyAddress = companyAddress;
        this.companyPhone = companyPhone;
    }

    /**
     * Records the storage key of the rendered PDF.
     */
    public void attachPdf(String storageKey) {
        this.pdfUrl = storageKey;
    }
}