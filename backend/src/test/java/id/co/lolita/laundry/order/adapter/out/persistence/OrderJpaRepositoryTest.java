package id.co.lolita.laundry.order.adapter.out.persistence;

import id.co.lolita.laundry.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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

        var result = repository.findOpenDeliveries();

        // DONE (ready) first ordered by oldest date, then the rest; DELIVERED excluded.
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
}