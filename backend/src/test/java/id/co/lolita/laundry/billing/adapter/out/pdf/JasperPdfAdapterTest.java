package id.co.lolita.laundry.billing.adapter.out.pdf;

import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.BillingOrderRow;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.InvoiceItemRow;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.MonthlyBillingDocument;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.OrderInvoiceDocument;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real JasperReports pipeline: compiling the .jrxml templates at runtime,
 * filling them, and exporting to PDF. Asserts the output is a non-trivial PDF document.
 */
class JasperPdfAdapterTest {

    private final JasperPdfAdapter adapter = new JasperPdfAdapter();

    private static boolean isPdf(byte[] bytes) {
        return bytes.length > 4 && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
    }

    @Test
    void rendersOrderInvoicePdf() {
        var doc = new OrderInvoiceDocument(
                "INV-PBS-20260601-001", "1 Juni 2026", "Pasar Baru Square", "PBS",
                "PBS-20260601-001", "1 Juni 2026", "Treatment", "Room Linen",
                List.of(
                        new InvoiceItemRow("Sheet King", "Pcs", "10", "Rp 5.000", "Rp 100.000"),
                        new InvoiceItemRow("Bath Towel", "Pcs", "5", "Rp 3.000", "Rp 30.000")),
                "Rp 130.000");

        byte[] pdf = adapter.renderOrderInvoice(doc);

        assertThat(pdf).isNotEmpty();
        assertThat(isPdf(pdf)).as("output is a PDF document").isTrue();
    }

    @Test
    void rendersMonthlyBillingPdf() {
        var doc = new MonthlyBillingDocument(
                "BILL-PBS-202606-RL", "Juni 2026", "Pasar Baru Square", "PBS", "Room Linen", "30 Juni 2026",
                List.of(
                        new BillingOrderRow("PBS-20260601-001", "01/06/2026", "Rp 100.000"),
                        new BillingOrderRow("PBS-20260615-002", "15/06/2026", "Rp 250.000")),
                "Rp 350.000");

        byte[] pdf = adapter.renderMonthlyBilling(doc);

        assertThat(pdf).isNotEmpty();
        assertThat(isPdf(pdf)).as("output is a PDF document").isTrue();
    }
}