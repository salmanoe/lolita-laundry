package id.co.lolita.laundry.billing.domain.port.out;

import id.co.lolita.laundry.billing.domain.MonthlyBilling;

import java.util.List;
import java.util.Optional;

public interface MonthlyBillingRepository {

    MonthlyBilling save(MonthlyBilling billing);

    Optional<MonthlyBilling> findById(Long id);

    /**
     * Newest period first; any null filter is dropped.
     */
    List<MonthlyBilling> findAll(Long clientId, Integer year, Integer month);

    /**
     * The existing billing for a client/department/period, if any. {@code departmentId} is
     * null for COMBINED clients. Backs the regeneration policy (replace DRAFT, reject ISSUED).
     */
    Optional<MonthlyBilling> findExisting(Long clientId, Long departmentId, int year, int month);

    void deleteById(Long id);
}