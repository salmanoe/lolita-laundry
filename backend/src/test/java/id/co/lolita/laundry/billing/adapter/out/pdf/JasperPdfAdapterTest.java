package id.co.lolita.laundry.billing.adapter.out.pdf;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.CompanyHeader;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.InvoiceItemRow;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.MonthlyBillingDocument;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.OrderInvoiceDocument;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real JasperReports pipeline: building the {@link net.sf.jasperreports.engine.design.JasperDesign}
 * layouts programmatically, compiling + filling them, and exporting to PDF (OpenPDF backend).
 * Beyond asserting the output is a PDF, it extracts the page text and checks that the invoice
 * <em>format</em> carries the fields the real Lolita template requires — letterhead, the INVOICE
 * title, the meta block, the item table / single period line, totals, Terbilang and the bank
 * block — so a layout regression that drops a field is caught here, not by eyeballing.
 */
class JasperPdfAdapterTest {

    private final JasperPdfAdapter adapter = new JasperPdfAdapter();

    private static final CompanyHeader COMPANY = new CompanyHeader("Lolita Laundry",
            "Jl. Sukaraja No. 318 Bandung", "082318359775", "Alban Valentino Ramatir",
            "Bank BCA", "4061792362", "Lolita Laundry");

    private static boolean isPdf(byte[] bytes) {
        return bytes.length > 4 && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
    }

    /**
     * Concatenated text of every page in the rendered PDF.
     */
    private static String textOf(byte[] pdf) {
        try {
            PdfReader reader = new PdfReader(pdf);
            try {
                var extractor = new PdfTextExtractor(reader);
                var sb = new StringBuilder();
                for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                    sb.append(extractor.getTextFromPage(page)).append('\n');
                }
                return sb.toString();
            } finally {
                reader.close();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read PDF text", e);
        }
    }

    @Test
    void rendersOrderInvoicePdf_withItemizedLayout() {
        var doc = new OrderInvoiceDocument(
                COMPANY,
                "INV-PBS-20260601-001", "1 Juni 2026", "Pasar Baru Square", "PBS",
                "PBS-20260601-001", "1 Juni 2026", "Treatment", "Room Linen",
                List.of(
                        new InvoiceItemRow("Sheet King", "Pcs", "10", "Rp 5.000", "Rp 100.000"),
                        new InvoiceItemRow("Bath Towel", "Pcs", "5", "Rp 3.000", "Rp 30.000")),
                "Rp 130.000");

        byte[] pdf = adapter.renderOrderInvoice(doc);

        assertThat(isPdf(pdf)).as("output is a PDF document").isTrue();

        String text = textOf(pdf);
        assertThat(text)
                .as("letterhead + title")
                .contains("Lolita Laundry")
                .contains("Jl. Sukaraja No. 318 Bandung")
                .contains("082318359775")
                .contains("INVOICE")
                .as("meta block")
                .contains("INV-PBS-20260601-001")
                .contains("Pasar Baru Square (PBS)")
                .contains("PBS-20260601-001")
                .contains("Treatment")
                .contains("Room Linen")
                .as("item table columns")
                .contains("Barang").contains("Satuan").contains("Qty").contains("Harga").contains("Subtotal")
                .as("line items")
                .contains("Sheet King").contains("Bath Towel")
                .contains("Rp 100.000").contains("Rp 30.000")
                .as("itemized total, no bank block")
                .contains("TOTAL")
                .contains("Rp 130.000")
                .doesNotContain("Terbilang")
                .doesNotContain("Please Transfer To");
    }

    @Test
    void rendersMonthlyBillingPdf_asClientFacingInvoice() {
        var doc = new MonthlyBillingDocument(
                COMPANY,
                "BILL-PBS-202606-RL", "Pasar Baru Square", "Room Linen", "01/06/26", "2 Days",
                "Laundry Periode 1 June - 30 June 2026", "Rp 350.000",
                "Tiga Ratus Lima Puluh Ribu Rupiah");

        byte[] pdf = adapter.renderMonthlyBilling(doc);

        assertThat(isPdf(pdf)).as("output is a PDF document").isTrue();

        String text = textOf(pdf);
        assertThat(text)
                .as("letterhead + title")
                .contains("Lolita Laundry")
                .contains("INVOICE")
                .as("hotel + meta block")
                .contains("Nama Hotel")
                .contains("Pasar Baru Square")
                .contains("Room Linen")
                .contains("Number").contains("BILL-PBS-202606-RL")
                .contains("Date").contains("01/06/26")
                .contains("Payment").contains("2 Days")
                .as("single period line + subtotal")
                .contains("Description")
                .contains("Laundry Periode 1 June - 30 June 2026")
                .contains("SUB TOTAL")
                .contains("Rp 350.000")
                .as("Terbilang amount-in-words")
                .contains("Terbilang")
                .contains("Tiga Ratus Lima Puluh Ribu Rupiah")
                .as("bank-transfer block")
                .contains("Please Transfer To")
                .contains("Bank BCA")
                .contains("No. Rekening : 4061792362")
                .contains("Nama Pemilik Rekening : Lolita Laundry");
    }
}