package id.co.lolita.laundry.billing.adapter.out.persistence;

import id.co.lolita.laundry.billing.domain.OrderInvoice;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "order_invoices")
@Getter
@Setter
@NoArgsConstructor
class OrderInvoiceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 30)
    private String invoiceNumber;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "pdf_url", columnDefinition = "text")
    private String pdfUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Company letterhead frozen at creation (see OrderInvoice). Nullable in the schema for
    // legacy rows, but always set on new invoices.
    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(name = "company_address", length = 200)
    private String companyAddress;

    @Column(name = "company_phone", length = 30)
    private String companyPhone;

    static OrderInvoiceJpaEntity fromDomain(OrderInvoice i) {
        var e = new OrderInvoiceJpaEntity();
        e.id = i.getId();
        e.invoiceNumber = i.getInvoiceNumber();
        e.orderId = i.getOrderId();
        e.clientId = i.getClientId();
        e.invoiceDate = i.getInvoiceDate();
        e.subtotal = i.getSubtotal();
        e.pdfUrl = i.getPdfUrl();
        e.createdAt = i.getCreatedAt();
        e.companyName = i.getCompanyName();
        e.companyAddress = i.getCompanyAddress();
        e.companyPhone = i.getCompanyPhone();
        return e;
    }

    OrderInvoice toDomain() {
        return new OrderInvoice(id, invoiceNumber, orderId, clientId, invoiceDate, subtotal, pdfUrl, createdAt,
                companyName, companyAddress, companyPhone);
    }
}