package id.co.lolita.laundry.billing.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BillingCycleTest {

    private static final BillingCycle CUTOFF_25 = BillingCycle.of(25);

    // ── calendar (the default for every client) ──

    @Test
    void calendar_mapsEveryDateToItsOwnMonth() {
        assertThat(BillingCycle.CALENDAR.periodOf(LocalDate.of(2026, 8, 1))).isEqualTo(YearMonth.of(2026, 8));
        assertThat(BillingCycle.CALENDAR.periodOf(LocalDate.of(2026, 8, 25))).isEqualTo(YearMonth.of(2026, 8));
        assertThat(BillingCycle.CALENDAR.periodOf(LocalDate.of(2026, 8, 31))).isEqualTo(YearMonth.of(2026, 8));
    }

    @Test
    void calendar_spansFirstToLastDay() {
        assertThat(BillingCycle.CALENDAR.startOf(YearMonth.of(2026, 8))).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(BillingCycle.CALENDAR.endOf(YearMonth.of(2026, 8))).isEqualTo(LocalDate.of(2026, 8, 31));
        // short month
        assertThat(BillingCycle.CALENDAR.endOf(YearMonth.of(2026, 2))).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void of_nullYieldsCalendar() {
        assertThat(BillingCycle.of(null)).isEqualTo(BillingCycle.CALENDAR);
        assertThat(BillingCycle.CALENDAR.isCalendar()).isTrue();
        assertThat(CUTOFF_25.isCalendar()).isFalse();
    }

    // ── cut-off cycle: the 25th ──

    @Test
    void cutoff_onOrBeforeTheCutoffStaysInItsOwnMonth() {
        assertThat(CUTOFF_25.periodOf(LocalDate.of(2026, 8, 1))).isEqualTo(YearMonth.of(2026, 8));
        assertThat(CUTOFF_25.periodOf(LocalDate.of(2026, 8, 24))).isEqualTo(YearMonth.of(2026, 8));
        assertThat(CUTOFF_25.periodOf(LocalDate.of(2026, 8, 25))).isEqualTo(YearMonth.of(2026, 8));
    }

    @Test
    void cutoff_afterTheCutoffRollsIntoTheNextPeriod() {
        assertThat(CUTOFF_25.periodOf(LocalDate.of(2026, 8, 26))).isEqualTo(YearMonth.of(2026, 9));
        assertThat(CUTOFF_25.periodOf(LocalDate.of(2026, 8, 31))).isEqualTo(YearMonth.of(2026, 9));
    }

    @Test
    void cutoff_rollsAcrossTheYearBoundary() {
        assertThat(CUTOFF_25.periodOf(LocalDate.of(2026, 12, 26))).isEqualTo(YearMonth.of(2027, 1));
        assertThat(CUTOFF_25.periodOf(LocalDate.of(2026, 12, 25))).isEqualTo(YearMonth.of(2026, 12));
    }

    @Test
    void cutoff_periodRunsFromTheDayAfterTheCutoffThroughTheCutoff() {
        assertThat(CUTOFF_25.startOf(YearMonth.of(2026, 8))).isEqualTo(LocalDate.of(2026, 7, 26));
        assertThat(CUTOFF_25.endOf(YearMonth.of(2026, 8))).isEqualTo(LocalDate.of(2026, 8, 25));
    }

    @Test
    void cutoff_januaryPeriodStartsInThePreviousYear() {
        assertThat(CUTOFF_25.startOf(YearMonth.of(2027, 1))).isEqualTo(LocalDate.of(2026, 12, 26));
        assertThat(CUTOFF_25.endOf(YearMonth.of(2027, 1))).isEqualTo(LocalDate.of(2027, 1, 25));
    }

    @Test
    void cutoff_28ResolvesCleanlyAroundFebruary() {
        var cycle = BillingCycle.of(28);
        // The March period starts the day after February's cut-off — no clamping needed.
        assertThat(cycle.startOf(YearMonth.of(2026, 3))).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(cycle.endOf(YearMonth.of(2026, 2))).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(cycle.periodOf(LocalDate.of(2026, 2, 28))).isEqualTo(YearMonth.of(2026, 2));
        assertThat(cycle.periodOf(LocalDate.of(2026, 3, 1))).isEqualTo(YearMonth.of(2026, 3));
    }

    // ── every date lands on exactly one period, and periods tile without gaps ──

    @Test
    void periodsTileTheCalendarWithoutGapsOrOverlaps() {
        var cycle = CUTOFF_25;
        for (var date = LocalDate.of(2026, 1, 1); date.isBefore(LocalDate.of(2027, 6, 1)); date = date.plusDays(1)) {
            var period = cycle.periodOf(date);
            assertThat(date).isBetween(cycle.startOf(period), cycle.endOf(period));
            // the previous period ends the day before this one starts
            assertThat(cycle.endOf(period.minusMonths(1))).isEqualTo(cycle.startOf(period).minusDays(1));
        }
    }

    // ── guard ──

    @Test
    void rejectsCutoffOutsideOneToTwentyEight() {
        assertThatThrownBy(() -> BillingCycle.of(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BillingCycle.of(29)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BillingCycle.of(31)).isInstanceOf(IllegalArgumentException.class);
    }
}
