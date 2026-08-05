package id.co.lolita.laundry.settings.adapter.out.persistence;

import id.co.lolita.laundry.settings.domain.BankAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence for a company bank account. At most one row may have {@code is_default = true};
 * that is enforced by a partial unique index in {@code V17}, with the service clearing the
 * previous default in the same transaction.
 */
@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
class BankAccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String label;

    @Column(nullable = false, length = 100)
    private String beneficiary;

    @Column(name = "bank_name", nullable = false, length = 50)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "account_holder", nullable = false, length = 100)
    private String accountHolder;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAccount;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    static BankAccountJpaEntity fromDomain(BankAccount a) {
        var e = new BankAccountJpaEntity();
        e.id = a.getId();
        e.applyScalars(a);
        return e;
    }

    void applyScalars(BankAccount a) {
        this.label = a.getLabel();
        this.beneficiary = a.getBeneficiary();
        this.bankName = a.getBankName();
        this.accountNumber = a.getAccountNumber();
        this.accountHolder = a.getAccountHolder();
        this.defaultAccount = a.isDefaultAccount();
        this.active = a.isActive();
        this.sortOrder = a.getSortOrder();
    }

    BankAccount toDomain() {
        return new BankAccount(id, label, beneficiary, bankName, accountNumber, accountHolder,
                defaultAccount, active, sortOrder);
    }
}
