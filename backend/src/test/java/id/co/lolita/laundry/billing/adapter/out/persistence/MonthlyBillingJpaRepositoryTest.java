package id.co.lolita.laundry.billing.adapter.out.persistence;

import id.co.lolita.laundry.billing.domain.BillingStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the billing lookup queries: {@code findExisting} distinguishing a COMBINED
 * (null-department) billing from per-department billings, and the optional-filter search.
 */
@DataJpaTest
class MonthlyBillingJpaRepositoryTest {

    @Autowired
    MonthlyBillingJpaRepository repository;

    @Autowired
    EntityManager em;

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
    void optimisticLock_incrementsVersion_andRejectsStaleUpdate() {
        // KI-6: @Version turns a concurrent same-row update into an OptimisticLockingFailureException
        // instead of a silent lost update.
        var saved = repository.saveAndFlush(billing("BILL-AYI-202606", 1L, null, 2026, 6));
        Long id = saved.getId();
        assertThat(saved.getVersion()).isZero();
        em.clear();

        // One writer loads the row, then is detached holding version 0.
        var stale = repository.findById(id).orElseThrow();
        em.detach(stale);

        // Another writer bumps the same row to version 1.
        var fresh = repository.findById(id).orElseThrow();
        fresh.setTotal(new BigDecimal("123.00"));
        repository.saveAndFlush(fresh);
        assertThat(fresh.getVersion()).isEqualTo(1L);
        em.clear();

        // The stale writer (still version 0) must now lose.
        stale.setTotal(new BigDecimal("456.00"));
        assertThatThrownBy(() -> repository.saveAndFlush(stale))
                .isInstanceOf(OptimisticLockingFailureException.class);
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