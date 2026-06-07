package id.co.lolita.laundry.billing.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface MonthlyBillingJpaRepository extends JpaRepository<MonthlyBillingJpaEntity, Long> {

    /**
     * Lists billings with optional filters — any null parameter drops its constraint.
     * Newest period first, then most recently created.
     */
    @Query("""
            SELECT b FROM MonthlyBillingJpaEntity b
            WHERE (:clientId is null or b.clientId = :clientId)
              AND (:year     is null or b.periodYear = :year)
              AND (:month    is null or b.periodMonth = :month)
            ORDER BY b.periodYear DESC, b.periodMonth DESC, b.createdAt DESC
            """)
    List<MonthlyBillingJpaEntity> search(
            @Param("clientId") Long clientId,
            @Param("year") Integer year,
            @Param("month") Integer month);

    /**
     * The existing billing for a client/department/period. A null {@code departmentId} matches
     * the COMBINED (department-less) billing.
     */
    @Query("""
            SELECT b FROM MonthlyBillingJpaEntity b
            WHERE b.clientId = :clientId
              AND ((:departmentId is null and b.departmentId is null) or b.departmentId = :departmentId)
              AND b.periodYear = :year
              AND b.periodMonth = :month
            """)
    Optional<MonthlyBillingJpaEntity> findExisting(
            @Param("clientId") Long clientId,
            @Param("departmentId") Long departmentId,
            @Param("year") int year,
            @Param("month") int month);

    /** The billing whose lines include the given order (at most one — an order is billed once). */
    @Query("SELECT l.billing FROM MonthlyBillingLineJpaEntity l WHERE l.orderId = :orderId")
    Optional<MonthlyBillingJpaEntity> findByOrderLine(@Param("orderId") Long orderId);
}