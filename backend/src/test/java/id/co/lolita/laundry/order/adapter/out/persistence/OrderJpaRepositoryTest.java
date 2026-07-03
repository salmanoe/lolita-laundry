package id.co.lolita.laundry.order.adapter.out.persistence;

import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the per-client-per-day order count (which drives the sequence number) and the
 * optional-filter search query. Same package as the package-private entity/repository.
 */
@DataJpaTest
class OrderJpaRepositoryTest {

    @Autowired
    OrderJpaRepository repository;

    private OrderJpaEntity order(String number, long clientId, LocalDate date, OrderStatus status) {
        var e = new OrderJpaEntity();
        e.setOrderNumber(number);
        e.setClientId(clientId);
        e.setOrderDate(date);
        e.setStatus(status);
        e.setPricingMultiplier(BigDecimal.ONE);
        e.setSubmittedByName("Staff");
        e.setCreatedAt(Instant.now());
        return e;
    }

    @Test
    void countByClientIdAndOrderDate_countsOnlyThatClientAndDate() {
        var today = LocalDate.now();
        repository.save(order("AYI-1", 1L, today, OrderStatus.RECEIVED));
        repository.save(order("AYI-2", 1L, today, OrderStatus.RECEIVED));
        repository.save(order("AYI-3", 1L, today.minusDays(1), OrderStatus.RECEIVED));
        repository.save(order("PBS-1", 2L, today, OrderStatus.RECEIVED));

        assertThat(repository.countByClientIdAndOrderDate(1L, today)).isEqualTo(2);
        assertThat(repository.countByClientIdAndOrderDate(2L, today)).isEqualTo(1);
    }

    @Test
    void findOpenDeliveries_returnsAllNonDelivered_readyFirst() {
        var today = LocalDate.now();
        // Open pool: every order not yet DELIVERED, regardless of which driver (no assignment).
        repository.save(order("AYI-1", 1L, today, OrderStatus.PROCESSING));
        repository.save(order("AYI-2", 1L, today.minusDays(1), OrderStatus.DONE));
        repository.save(order("AYI-3", 1L, today, OrderStatus.DELIVERED));
        repository.save(order("AYI-4", 2L, today, OrderStatus.DONE));
        repository.save(order("AYI-5", 1L, today, OrderStatus.CANCELLED));

        var result = repository.findOpenDeliveries();

        // DONE (ready) first ordered by oldest date, then the rest; DELIVERED and CANCELLED excluded.
        assertThat(result).extracting(OrderJpaEntity::getOrderNumber)
                .containsExactly("AYI-2", "AYI-4", "AYI-1");
    }

    @Test
    void search_appliesOnlyTheSuppliedFilters() {
        var today = LocalDate.now();
        repository.save(order("AYI-1", 1L, today, OrderStatus.RECEIVED));
        repository.save(order("AYI-2", 1L, today, OrderStatus.DONE));
        repository.save(order("PBS-1", 2L, today, OrderStatus.RECEIVED));

        var pageable = PageRequest.of(0, 10);

        // No filters → everything.
        assertThat(repository.search(null, null, null, null, pageable).getTotalElements()).isEqualTo(3);

        // Client filter.
        assertThat(repository.search(1L, null, null, null, pageable).getTotalElements()).isEqualTo(2);

        // Client + status filter.
        var received = repository.search(1L, OrderStatus.RECEIVED, null, null, pageable);
        assertThat(received.getTotalElements()).isEqualTo(1);
        assertThat(received.getContent().getFirst().getOrderNumber()).isEqualTo("AYI-1");

        // Date window that excludes today.
        assertThat(repository.search(null, null, null, today.minusDays(1), pageable).getTotalElements())
                .isZero();
    }

    @Test
    void findBillableInPeriod_excludesCancelledAndOutOfRange_oldestFirst() {
        var today = LocalDate.now();
        repository.save(order("AYI-1", 1L, today, OrderStatus.RECEIVED));
        repository.save(order("PBS-1", 2L, today.minusDays(1), OrderStatus.DONE));
        repository.save(order("AYI-2", 1L, today, OrderStatus.CANCELLED));      // excluded
        repository.save(order("AYI-3", 1L, today.minusDays(5), OrderStatus.RECEIVED)); // out of range

        var result = repository.findBillableInPeriod(today.minusDays(2), today);

        assertThat(result).extracting(OrderJpaEntity::getOrderNumber)
                .containsExactly("PBS-1", "AYI-1");
    }

    @Test
    void save_persistsTreatmentCorrection_onUpdate() {
        // Regression: the update path (applyScalars) must persist a corrected pricing_multiplier,
        // not just the re-priced line subtotals — otherwise the order is left in an inconsistent
        // state (multiplier 1.0 with ×2 subtotals). Round-trips through the real adapter.
        var adapter = new OrderJpaAdapter(repository);
        var created = adapter.save(Order.create(
                "PBS-20260101-001", 6L, LocalDate.now(), null, BigDecimal.ONE, "Staff", null, null,
                List.of(new Order.NewLine(1L, new BigDecimal("2"), new BigDecimal("1000"), 1L)),
                Instant.now()));
        assertThat(created.getPricingMultiplier()).isEqualByComparingTo("1.0");
        assertThat(created.total()).isEqualByComparingTo("2000.00");

        // Reload, flip Reguler → Treatment (×2) with no new item list, re-save.
        var reloaded = adapter.findById(created.getId()).orElseThrow();
        reloaded.edit(null, new BigDecimal("2.0"), null, null, null);
        var saved = adapter.save(reloaded);

        assertThat(saved.getPricingMultiplier()).isEqualByComparingTo("2.0");
        assertThat(saved.total()).isEqualByComparingTo("4000.00");
        // Fresh read from the DB confirms it persisted.
        var fresh = adapter.findById(created.getId()).orElseThrow();
        assertThat(fresh.getPricingMultiplier()).isEqualByComparingTo("2.0");
        assertThat(fresh.getLineItems().getFirst().subtotal()).isEqualByComparingTo("4000.00");
        assertThat(fresh.getLineItems().getFirst().priceAtOrder()).isEqualByComparingTo("1000");
    }

    @Test
    void countByOrderDate_andCountByStatusIn() {
        var today = LocalDate.now();
        repository.save(order("AYI-1", 1L, today, OrderStatus.RECEIVED));
        repository.save(order("AYI-2", 1L, today, OrderStatus.PROCESSING));
        repository.save(order("AYI-3", 1L, today.minusDays(1), OrderStatus.DONE));

        assertThat(repository.countByOrderDate(today)).isEqualTo(2);
        assertThat(repository.countByStatusIn(java.util.List.of(OrderStatus.RECEIVED, OrderStatus.PROCESSING)))
                .isEqualTo(2);
        assertThat(repository.countByStatusIn(java.util.List.of(OrderStatus.DONE))).isEqualTo(1);
    }
}