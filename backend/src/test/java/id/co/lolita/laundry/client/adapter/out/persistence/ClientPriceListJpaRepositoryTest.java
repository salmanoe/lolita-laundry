package id.co.lolita.laundry.client.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the append-only price-list lookup queries — the core pricing invariant:
 * a price change is a new row with a newer effective date, and lookups must always
 * resolve to the latest row effective on or before the requested date.
 *
 * <p>Same package as the entity/repository because both are package-private.
 */
@DataJpaTest
class ClientPriceListJpaRepositoryTest {

    @Autowired
    ClientPriceListJpaRepository repository;

    private static final long CLIENT = 1L;

    private ClientPriceListJpaEntity price(long itemId, String amount, LocalDate date) {
        var e = new ClientPriceListJpaEntity();
        e.setClientId(CLIENT);
        e.setItemId(itemId);
        e.setPricePerUnit(new BigDecimal(amount));
        e.setEffectiveDate(date);
        e.setCreatedAt(Instant.now());
        return e;
    }

    @Test
    void findEffectivePrice_returnsLatestRowOnOrBeforeDate() {
        var today = LocalDate.now();
        repository.save(price(10, "5000", today.minusMonths(5)));
        repository.save(price(10, "6000", today.minusMonths(3)));
        repository.save(price(10, "7000", today.minusMonths(1)));

        // As of 2 months ago, the effective row is the one from 3 months ago.
        var twoMonthsAgo = repository.findEffectivePrice(CLIENT, 10L, today.minusMonths(2));
        assertThat(twoMonthsAgo).isPresent();
        assertThat(twoMonthsAgo.get().getPricePerUnit()).isEqualByComparingTo("6000");

        // As of today, the latest row applies.
        var now = repository.findEffectivePrice(CLIENT, 10L, today);
        assertThat(now).isPresent();
        assertThat(now.get().getPricePerUnit()).isEqualByComparingTo("7000");
    }

    @Test
    void findEffectivePrice_emptyWhenNothingEffectiveYet() {
        var today = LocalDate.now();
        repository.save(price(10, "5000", today.minusMonths(1)));

        var before = repository.findEffectivePrice(CLIENT, 10L, today.minusMonths(6));
        assertThat(before).isEmpty();
    }

    @Test
    void findCurrentPrices_returnsOneLatestRowPerItemExcludingFutureDates() {
        var today = LocalDate.now();
        repository.save(price(10, "5000", today.minusMonths(5)));
        repository.save(price(10, "7000", today.minusMonths(1)));   // newer for item 10
        repository.save(price(20, "3000", today.minusMonths(3)));
        repository.save(price(30, "9999", today.plusYears(1)));     // future — must be excluded

        var current = repository.findCurrentPrices(CLIENT, today);

        assertThat(current).hasSize(2);
        assertThat(current).extracting(ClientPriceListJpaEntity::getItemId).containsExactly(10L, 20L);
        assertThat(current.get(0).getPricePerUnit()).isEqualByComparingTo("7000");
        assertThat(current.get(1).getPricePerUnit()).isEqualByComparingTo("3000");
    }

    @Test
    void findCurrentPrices_includesRowEffectiveToday() {
        var today = LocalDate.now();
        repository.save(price(10, "5000", today.minusMonths(1)));
        repository.save(price(10, "7000", today));   // effective today — must be the current price

        var current = repository.findCurrentPrices(CLIENT, today);

        assertThat(current).hasSize(1);
        assertThat(current.get(0).getItemId()).isEqualTo(10L);
        assertThat(current.get(0).getPricePerUnit()).isEqualByComparingTo("7000");
    }
}
