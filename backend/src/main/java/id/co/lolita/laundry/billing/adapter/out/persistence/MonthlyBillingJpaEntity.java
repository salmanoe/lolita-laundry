package id.co.lolita.laundry.billing.adapter.out.persistence;

import id.co.lolita.laundry.billing.domain.BillingStatus;
import id.co.lolita.laundry.billing.domain.MonthlyBilling;
import id.co.lolita.laundry.billing.domain.MonthlyBillingLine;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "monthly_billings")
@Getter
@Setter
@NoArgsConstructor
class MonthlyBillingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "billing_number", nullable = false, unique = true, length = 30)
    private String billingNumber;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private BillingStatus status;

    @Column(name = "pdf_url", columnDefinition = "text")
    private String pdfUrl;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonthlyBillingLineJpaEntity> lines = new ArrayList<>();

    static MonthlyBillingJpaEntity newFromDomain(MonthlyBilling b) {
        var e = new MonthlyBillingJpaEntity();
        e.id = b.getId();
        e.billingNumber = b.getBillingNumber();
        e.clientId = b.getClientId();
        e.departmentId = b.getDepartmentId();
        e.periodYear = b.getPeriodYear();
        e.periodMonth = b.getPeriodMonth();
        e.invoiceDate = b.getInvoiceDate();
        e.total = b.getTotal();
        e.status = b.getStatus();
        e.pdfUrl = b.getPdfUrl();
        e.notes = b.getNotes();
        e.createdAt = b.getCreatedAt();
        for (var line : b.getLines()) {
            var le = MonthlyBillingLineJpaEntity.fromDomain(line);
            le.setBilling(e);
            e.lines.add(le);
        }
        return e;
    }

    /** Copies the only mutable fields (status, attached PDF) onto a managed entity. */
    void applyMutable(MonthlyBilling b) {
        this.status = b.getStatus();
        this.pdfUrl = b.getPdfUrl();
    }

    MonthlyBilling toDomain() {
        List<MonthlyBillingLine> domainLines = lines.stream()
                .map(MonthlyBillingLineJpaEntity::toDomain).toList();
        return new MonthlyBilling(id, billingNumber, clientId, departmentId, periodYear, periodMonth,
                invoiceDate, total, status, pdfUrl, notes, createdAt, domainLines);
    }
}