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

    // Optimistic-lock guard (KI-6): a concurrent stale update throws
    // OptimisticLockingFailureException instead of silently losing the other write.
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "billing_number", nullable = false, unique = true, length = 30)
    private String billingNumber;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "department_name", length = 100)
    private String departmentName;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    /**
     * The calendar range the period covers, resolved from the client's billing cycle. A DRAFT
     * re-resolves it on every sync; ISSUE freezes it. Stored rather than recomputed at render time
     * so changing a client's cycle can never rewrite the period wording of a document already sent.
     */
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

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


    // Company snapshot, frozen at ISSUE (null while DRAFT — rendered from the live profile).
    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(name = "company_address", length = 200)
    private String companyAddress;

    @Column(name = "company_phone", length = 30)
    private String companyPhone;

    @Column(name = "bank_beneficiary", length = 100)
    private String bankBeneficiary;

    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "bank_account", length = 50)
    private String bankAccount;

    @Column(name = "bank_holder", length = 100)
    private String bankHolder;

    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonthlyBillingLineJpaEntity> lines = new ArrayList<>();

    static MonthlyBillingJpaEntity newFromDomain(MonthlyBilling b) {
        var e = new MonthlyBillingJpaEntity();
        e.id = b.getId();
        e.billingNumber = b.getBillingNumber();
        e.clientId = b.getClientId();
        e.departmentId = b.getDepartmentId();
        e.departmentName = b.getDepartmentName();
        e.periodYear = b.getPeriodYear();
        e.periodMonth = b.getPeriodMonth();
        e.periodStart = b.getPeriodStart();
        e.periodEnd = b.getPeriodEnd();
        e.invoiceDate = b.getInvoiceDate();
        e.total = b.getTotal();
        e.status = b.getStatus();
        e.pdfUrl = b.getPdfUrl();
        e.notes = b.getNotes();
        e.createdAt = b.getCreatedAt();
        e.copyCompanyFrom(b);
        for (var line : b.getLines()) {
            var le = MonthlyBillingLineJpaEntity.fromDomain(line);
            le.setBilling(e);
            e.lines.add(le);
        }
        return e;
    }

    /**
     * Copies the mutable fields (status, PDF, total, invoice date, company snapshot) and
     * reconciles the line set in place. The company snapshot is filled in when the billing is
     * ISSUED, and a cut-off-cycle billing is re-stamped with its real issue date at the same
     * moment. The period identity and its frozen date range never change.
     */
    void applyMutable(MonthlyBilling b) {
        this.status = b.getStatus();
        this.pdfUrl = b.getPdfUrl();
        this.total = b.getTotal();
        this.invoiceDate = b.getInvoiceDate();
        // A DRAFT re-resolves its period range from the client's current billing cycle; ISSUE
        // freezes it, and the domain rejects a reposition after that.
        this.periodStart = b.getPeriodStart();
        this.periodEnd = b.getPeriodEnd();
        copyCompanyFrom(b);
        syncLines(b);
    }

    private void copyCompanyFrom(MonthlyBilling b) {
        this.companyName = b.getCompanyName();
        this.companyAddress = b.getCompanyAddress();
        this.companyPhone = b.getCompanyPhone();
        this.bankBeneficiary = b.getBankBeneficiary();
        this.bankName = b.getBankName();
        this.bankAccount = b.getBankAccount();
        this.bankHolder = b.getBankHolder();
    }

    /**
     * Reconciles this entity's line rows with the domain billing's lines, in place (update
     * existing, add new, drop removed) so updates avoid the UNIQUE(billing_id, order_id)
     * collision a clear-and-reinsert would risk, and keep stable line ids where possible.
     */
    private void syncLines(MonthlyBilling b) {
        var desired = b.getLines();
        this.lines.removeIf(le -> desired.stream().noneMatch(d -> d.orderId().equals(le.getOrderId())));
        for (var d : desired) {
            var match = this.lines.stream()
                    .filter(le -> le.getOrderId().equals(d.orderId())).findFirst().orElse(null);
            if (match != null) {
                match.setOrderNumber(d.orderNumber());
                match.setOrderDate(d.orderDate());
                match.setSubtotal(d.subtotal());
            } else {
                var ne = MonthlyBillingLineJpaEntity.fromDomain(d);
                ne.setBilling(this);
                this.lines.add(ne);
            }
        }
    }

    MonthlyBilling toDomain() {
        List<MonthlyBillingLine> domainLines = lines.stream()
                .map(MonthlyBillingLineJpaEntity::toDomain).toList();
        var billing = new MonthlyBilling(id, billingNumber, clientId, departmentId, departmentName, periodYear,
                periodMonth, periodStart, periodEnd, invoiceDate, total, status, pdfUrl, notes, createdAt,
                domainLines);
        billing.captureCompany(companyName, companyAddress, companyPhone, bankBeneficiary, bankName, bankAccount,
                bankHolder);
        return billing;
    }
}