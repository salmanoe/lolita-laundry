package id.co.lolita.laundry.billing.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Terbilang (Indonesian amount-in-words) and the monthly-invoice period/date helpers.
 */
class BillingFormatsTest {

    @Test
    void terbilang_matchesTheRealInvoiceExample() {
        // From "invoice runi maret - invoice.csv": Rp3,079,000 → Terbilang.
        assertThat(BillingFormats.terbilang(new BigDecimal("3079000")))
                .isEqualTo("Tiga Juta Tujuh Puluh Sembilan Ribu Rupiah");
    }

    @Test
    void terbilang_handlesEdgeCases() {
        assertThat(BillingFormats.terbilang(BigDecimal.ZERO)).isEqualTo("Nol Rupiah");
        assertThat(BillingFormats.terbilang(new BigDecimal("1000"))).isEqualTo("Seribu Rupiah");
        assertThat(BillingFormats.terbilang(new BigDecimal("1500"))).isEqualTo("Seribu Lima Ratus Rupiah");
        assertThat(BillingFormats.terbilang(new BigDecimal("11"))).isEqualTo("Sebelas Rupiah");
        assertThat(BillingFormats.terbilang(new BigDecimal("21"))).isEqualTo("Dua Puluh Satu Rupiah");
        assertThat(BillingFormats.terbilang(new BigDecimal("100"))).isEqualTo("Seratus Rupiah");
        assertThat(BillingFormats.terbilang(new BigDecimal("215000")))
                .isEqualTo("Dua Ratus Lima Belas Ribu Rupiah");
        assertThat(BillingFormats.terbilang(new BigDecimal("1234567")))
                .isEqualTo("Satu Juta Dua Ratus Tiga Puluh Empat Ribu Lima Ratus Enam Puluh Tujuh Rupiah");
        // Rounds fractional rupiah to whole.
        assertThat(BillingFormats.terbilang(new BigDecimal("999.50"))).isEqualTo("Seribu Rupiah");
    }

    @Test
    void terbilang_handlesNegativeCreditTotals() {
        // KI-11: a credit-carrying DRAFT can have a negative total; spelling must not throw
        // (a negative array index) — it renders as "Minus …" so the ledger draft's PDF renders.
        assertThat(BillingFormats.terbilang(new BigDecimal("-3000")))
                .isEqualTo("Minus Tiga Ribu Rupiah");
        assertThat(BillingFormats.terbilang(new BigDecimal("-5"))).isEqualTo("Minus Lima Rupiah");
    }

    @Test
    void periodDescription_spansFirstToLastDayOfMonth() {
        assertThat(BillingFormats.periodDescription(2026, 6)).isEqualTo("Laundry Periode 1 June - 30 June 2026");
        assertThat(BillingFormats.periodDescription(2026, 2)).isEqualTo("Laundry Periode 1 February - 28 February 2026");
    }

    @Test
    void shortDateYy_isDayMonthTwoDigitYear() {
        assertThat(BillingFormats.shortDateYy(LocalDate.of(2026, 6, 1))).isEqualTo("01/06/26");
    }
}
