package id.co.lolita.laundry.billing.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Business rules of the MonthlyBilling aggregate: total aggregation, the one-way
 * DRAFT → ISSUED → PAID lifecycle, and the no-empty-billing guard.
 */
class MonthlyBillingTest {

    private static MonthlyBillingLine line(String orderNumber, String subtotal) {
        return MonthlyBillingLine.of(1L, orderNumber, LocalDate.of(2026, 6, 1), new BigDecimal(subtotal));
    }

    private static MonthlyBilling draftWith(String... subtotals) {
        var lines = java.util.Arrays.stream(subtotals).map(s -> line("AYI-20260601-001", s)).toList();
        return MonthlyBilling.generate("BILL-AYI-202606", 1L, null, null, 2026, 6, LocalDate.now(), lines);
    }

    @Test
    void generate_sumsLineSubtotalsAndStartsAsDraft() {
        var billing = draftWith("5000.00", "3000.50", "1500.00");

        assertThat(billing.getStatus()).isEqualTo(BillingStatus.DRAFT);
        assertThat(billing.getTotal()).isEqualByComparingTo("9500.50");
        assertThat(billing.getLines()).hasSize(3);
    }

    @Test
    void generate_rejectsEmptyPeriod() {
        assertThatThrownBy(() -> MonthlyBilling.generate(
                "BILL-AYI-202606", 1L, null, null, 2026, 6, LocalDate.now(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void advanceStatus_followsDraftIssuedPaid() {
        var billing = draftWith("1000.00");

        billing.advanceStatus(BillingStatus.ISSUED);
        assertThat(billing.getStatus()).isEqualTo(BillingStatus.ISSUED);
        billing.advanceStatus(BillingStatus.PAID);
        assertThat(billing.getStatus()).isEqualTo(BillingStatus.PAID);
    }

    @Test
    void advanceStatus_rejectsSkippingAndReversing() {
        var draft = draftWith("1000.00");
        assertThatThrownBy(() -> draft.advanceStatus(BillingStatus.PAID))     // skip ISSUED
                .isInstanceOf(IllegalArgumentException.class);

        var issued = draftWith("1000.00");
        issued.advanceStatus(BillingStatus.ISSUED);
        assertThatThrownBy(() -> issued.advanceStatus(BillingStatus.DRAFT))   // reverse
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void advanceStatus_rejectsIssuingNegativeTotalBilling() {
        // KI-11: a DRAFT carrying a net credit (negative total, e.g. a roll-forward credit off a
        // frozen bill) is a valid internal ledger but must not be issued as a client invoice.
        var credit = draftWith("-3000.00");
        assertThat(credit.getTotal()).isEqualByComparingTo("-3000.00");

        assertThatThrownBy(() -> credit.advanceStatus(BillingStatus.ISSUED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bersaldo negatif");
        assertThat(credit.getStatus()).isEqualTo(BillingStatus.DRAFT);   // stays a draft
    }

    @Test
    void advanceStatus_allowsIssuingOnceCreditNetsNonNegative() {
        // The same period after offsetting orders join: net total is positive → issuing is allowed.
        var netted = draftWith("-3000.00", "5000.00");
        assertThat(netted.getTotal()).isEqualByComparingTo("2000.00");

        netted.advanceStatus(BillingStatus.ISSUED);
        assertThat(netted.getStatus()).isEqualTo(BillingStatus.ISSUED);
    }

    @Test
    void attachPdf_recordsStorageKey() {
        var billing = draftWith("1000.00");
        billing.attachPdf("billings/BILL-AYI-202606.pdf");
        assertThat(billing.getPdfUrl()).isEqualTo("billings/BILL-AYI-202606.pdf");
    }
}