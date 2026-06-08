package id.co.lolita.laundry.billing.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The per-order invoice — one per delivered order, for every client. An itemized reference
 * document generated automatically when the order is delivered (driven by
 * {@code OrderDeliveredEvent}). The {@code subtotal} is the order total (the multiplier is
 * already baked into each line). {@code pdfUrl} is the storage key of the rendered PDF,
 * attached once the PDF is generated.
 */
@Getter
public class OrderInvoice {

    private final Long id;
    private final String invoiceNumber;
    private final Long orderId;
    private final Long clientId;
    private final LocalDate invoiceDate;
    private final BigDecimal subtotal;
    private String pdfUrl;            // storage object key — nullable until the PDF is rendered
    private final Instant createdAt;
    // Company letterhead frozen at creation — an order invoice is an immutable reference
    // document, so a later company-profile change must never rewrite it.
    private final String companyName;
    private final String companyAddress;
    private final String companyPhone;

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
     * Builds a new (unsaved) invoice for a delivered order, freezing the company letterhead as it
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
     * Records the storage key of the rendered PDF.
     */
    public void attachPdf(String storageKey) {
        this.pdfUrl = storageKey;
    }
}