package id.co.lolita.laundry.billing.adapter.out.persistence;

import id.co.lolita.laundry.billing.domain.BillingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the billing lookup queries: {@code findExisting} distinguishing a COMBINED
 * (null-department) billing from per-department billings, and the optional-filter search.
 */
@DataJpaTest
class MonthlyBillingJpaRepositoryTest {

    @Autowired
    MonthlyBillingJpaRepository repository;

    private MonthlyBillingJpaEntity billing(String number, long clientId, Long departmentId,
                                            int year, int month) {
        var e = new MonthlyBillingJpaEntity();
        e.setBillingNumber(number);
        e.setClientId(clientId);
        e.setDepartmentId(departmentId);
        e.setPeriodYear(year);
        e.setPeriodMonth(month);
        e.setInvoiceDate(LocalDate.of(year, month, 1));
        e.setTotal(new BigDecimal("10000.00"));
        e.setStatus(BillingStatus.DRAFT);
        e.setCreatedAt(Instant.now());
        return e;
    }

    @Test
    void findExisting_matchesCombinedBillingByNullDepartment() {
        repository.save(billing("BILL-AYI-202606", 1L, null, 2026, 6));
        repository.save(billing("BILL-PBS-202606-RL", 7L, 10L, 2026, 6));

        assertThat(repository.findExisting(1L, null, 2026, 6)).isPresent();
        assertThat(repository.findExisting(1L, null, 2026, 7)).isEmpty();   // wrong month
        // A department filter must not match the combined (null-department) billing.
        assertThat(repository.findExisting(1L, 10L, 2026, 6)).isEmpty();
    }

    @Test
    void findExisting_matchesPerDepartmentBilling() {
        repository.save(billing("BILL-PBS-202606-RL", 7L, 10L, 2026, 6));
        repository.save(billing("BILL-PBS-202606-FBL", 7L, 20L, 2026, 6));

        assertThat(repository.findExisting(7L, 10L, 2026, 6)).isPresent();
        assertThat(repository.findExisting(7L, 20L, 2026, 6)).isPresent();
        assertThat(repository.findExisting(7L, 30L, 2026, 6)).isEmpty();    // no such department
    }

    @Test
    void search_filtersByClientAndPeriod() {
        repository.save(billing("BILL-AYI-202606", 1L, null, 2026, 6));
        repository.save(billing("BILL-AYI-202605", 1L, null, 2026, 5));
        repository.save(billing("BILL-PBS-202606-RL", 7L, 10L, 2026, 6));

        assertThat(repository.search(1L, null, null)).hasSize(2);
        assertThat(repository.search(1L, 2026, 6)).hasSize(1);
        assertThat(repository.search(null, 2026, 6)).hasSize(2);
        assertThat(repository.search(null, null, null)).hasSize(3);
    }
}