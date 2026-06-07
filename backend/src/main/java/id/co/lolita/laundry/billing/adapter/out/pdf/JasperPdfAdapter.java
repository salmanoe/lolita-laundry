package id.co.lolita.laundry.billing.adapter.out.pdf;

import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders billing PDFs with JasperReports. The report layouts are built programmatically via
 * the {@link JasperDesign} API (not .jrxml) — this sidesteps JasperReports 7's stricter
 * template loader and keeps the layout version-stable and unit-testable. Each report is
 * compiled once and cached. Every value is a plain string already formatted by the
 * application layer, so report expressions are simple {@code $P{...}} / {@code $F{...}}
 * references with no locale or arithmetic logic.
 */
@Component
class JasperPdfAdapter implements InvoicePdfPort {

    private static final int PAGE_WIDTH = 595;          // A4 portrait at 72dpi
    private static final int MARGIN = 25;
    private static final int CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;   // 545
    private static final Color HEADER_BG = new Color(0xEE, 0xEE, 0xEE);
    private static final Color RULE = new Color(0xDD, 0xDD, 0xDD);

    // ── Fixed company letterhead / payment details (from the real Lolita invoice template) ──
    private static final String COMPANY_NAME = "Lolita Laundry";
    private static final String COMPANY_ADDRESS = "Jl. Sukaraja No. 318 Bandung";
    private static final String COMPANY_PHONE = "082318359775";
    private static final String BANK_BENEFICIARY = "Alban Valentino Ramatir";
    private static final String BANK_NAME = "Bank BCA";
    private static final String BANK_ACCOUNT = "4061792362";
    private static final String BANK_HOLDER = "Lolita Laundry";

    private volatile JasperReport orderInvoiceReport;
    private volatile JasperReport monthlyBillingReport;

    // ── InvoicePdfPort ──

    @Override
    public byte[] renderOrderInvoice(OrderInvoiceDocument doc) {
        Map<String, Object> params = getStringObjectMap(doc);

        List<Map<String, ?>> rows = new ArrayList<>();
        for (InvoiceItemRow item : doc.items()) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", item.name());
            row.put("unit", item.unit());
            row.put("quantity", item.quantity());
            row.put("unitPrice", item.unitPrice());
            row.put("subtotal", item.subtotal());
            rows.add(row);
        }
        return export(orderInvoiceReport(), params, rows);
    }

    private static @NonNull Map<String, Object> getStringObjectMap(OrderInvoiceDocument doc) {
        Map<String, Object> params = new HashMap<>();
        params.put("invoiceNumber", doc.invoiceNumber());
        params.put("invoiceDate", doc.invoiceDate());
        params.put("clientLine", clientLine(doc.clientName(), doc.clientCode()));
        params.put("orderNumber", doc.orderNumber());
        params.put("orderDate", doc.orderDate());
        params.put("orderTypeLabel", doc.orderTypeLabel());
        params.put("departmentName", doc.departmentName());
        params.put("total", doc.total());
        return params;
    }

    @Override
    public byte[] renderMonthlyBilling(MonthlyBillingDocument doc) {
        Map<String, Object> params = new HashMap<>();
        params.put("number", doc.number());
        params.put("clientName", doc.clientName());
        params.put("departmentName", doc.departmentName());
        params.put("invoiceDate", doc.invoiceDate());
        params.put("paymentTerms", doc.paymentTerms());
        params.put("periodDescription", doc.periodDescription());
        params.put("total", doc.total());
        params.put("amountInWords", doc.amountInWords());

        // The invoice is a single static "Laundry Periode" line — one (empty) record so the
        // title band, which holds the whole layout, renders exactly once.
        List<Map<String, ?>> rows = new ArrayList<>();
        rows.add(new HashMap<>());
        return export(monthlyBillingReport(), params, rows);
    }

    private static String clientLine(String name, String code) {
        return name + " (" + code + ")";
    }

    private byte[] export(JasperReport report, Map<String, Object> params, Collection<Map<String, ?>> rows) {
        try {
            JasperPrint print = JasperFillManager.fillReport(report, params, new JRMapCollectionDataSource(rows));
            var out = new ByteArrayOutputStream();
            var exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
            exporter.exportReport();
            return out.toByteArray();
        } catch (JRException e) {
            throw new IllegalStateException("Failed to render PDF", e);
        }
    }

    // ── compiled-report cache ──

    private JasperReport orderInvoiceReport() {
        if (orderInvoiceReport == null) {
            orderInvoiceReport = compile(buildOrderInvoiceDesign());
        }
        return orderInvoiceReport;
    }

    private JasperReport monthlyBillingReport() {
        if (monthlyBillingReport == null) {
            monthlyBillingReport = compile(buildMonthlyBillingDesign());
        }
        return monthlyBillingReport;
    }

    private static JasperReport compile(JasperDesign design) {
        try {
            return JasperCompileManager.compileReport(design);
        } catch (JRException e) {
            throw new IllegalStateException("Failed to compile report " + design.getName(), e);
        }
    }

    // ── report designs ──

    private JasperDesign buildOrderInvoiceDesign() {
        try {
            JasperDesign d = baseDesign("order_invoice");
            addParameters(d, "invoiceNumber", "invoiceDate", "clientLine", "orderNumber",
                    "orderDate", "orderTypeLabel", "departmentName", "total");
            addFields(d, "name", "unit", "quantity", "unitPrice", "subtotal");

            JRDesignBand title = band(172);
            // Shared letterhead (matches the monthly billing invoice) — itemized layout below.
            title.addElement(text(COMPANY_NAME, 0, 0, 320, 22, 15f, true, HorizontalTextAlignEnum.LEFT));
            title.addElement(text(COMPANY_ADDRESS, 0, 22, 320, 14, 9f, false, HorizontalTextAlignEnum.LEFT));
            title.addElement(text(COMPANY_PHONE, 0, 36, 320, 14, 9f, false, HorizontalTextAlignEnum.LEFT));
            title.addElement(text("INVOICE", 0, 58, CONTENT_WIDTH, 24, 18f, true, HorizontalTextAlignEnum.CENTER));
            title.addElement(rule(0, 88, CONTENT_WIDTH));
            labelValue(title, "No. Invoice", "invoiceNumber", 0, 96, true);
            labelValue(title, "Tanggal", "invoiceDate", 0, 114, false);
            labelValue(title, "Jenis", "orderTypeLabel", 0, 132, false);
            labelValue(title, "Klien", "clientLine", 280, 96, true);
            labelValue(title, "Departemen", "departmentName", 280, 114, false);
            labelValue(title, "No. Order", "orderNumber", 280, 132, false);
            labelValue(title, "Tgl Order", "orderDate", 280, 150, false);
            d.setTitle(title);

            JRDesignBand head = band(22);
            head.addElement(headerCell("Barang", 0, 210, HorizontalTextAlignEnum.LEFT));
            head.addElement(headerCell("Satuan", 210, 55, HorizontalTextAlignEnum.LEFT));
            head.addElement(headerCell("Qty", 265, 60, HorizontalTextAlignEnum.RIGHT));
            head.addElement(headerCell("Harga", 325, 105, HorizontalTextAlignEnum.RIGHT));
            head.addElement(headerCell("Subtotal", 430, 115, HorizontalTextAlignEnum.RIGHT));
            d.setColumnHeader(head);

            JRDesignBand detail = band(18);
            detail.addElement(fieldCell("name", 0, 210, HorizontalTextAlignEnum.LEFT));
            detail.addElement(fieldCell("unit", 210, 55, HorizontalTextAlignEnum.LEFT));
            detail.addElement(fieldCell("quantity", 265, 60, HorizontalTextAlignEnum.RIGHT));
            detail.addElement(fieldCell("unitPrice", 325, 105, HorizontalTextAlignEnum.RIGHT));
            detail.addElement(fieldCell("subtotal", 430, 115, HorizontalTextAlignEnum.RIGHT));
            detail.addElement(rule(0, 17, CONTENT_WIDTH, RULE));
            detailBand(d, detail);

            JRDesignBand summary = band(50);
            summary.addElement(rule(0, 2, CONTENT_WIDTH));
            summary.addElement(text("TOTAL", 265, 12, 160, 20, 12f, true, HorizontalTextAlignEnum.RIGHT));
            summary.addElement(paramCell("total", 430, 12, 115, 20, 12f, true, HorizontalTextAlignEnum.RIGHT));
            d.setSummary(summary);
            return d;
        } catch (JRException e) {
            throw new IllegalStateException("Failed to build order invoice design", e);
        }
    }

    /**
     * Monthly billing = the client-facing INVOICE, laid out to match the real Lolita template:
     * company letterhead, INVOICE title, hotel + number/date/payment block, a single
     * "Laundry Periode" line with the total, SUB TOTAL, Terbilang, bank-transfer details and a
     * signature. The whole document lives in the title band (one render), driven by a one-row
     * datasource. No detail/column-header bands.
     */
    private JasperDesign buildMonthlyBillingDesign() {
        try {
            JasperDesign d = baseDesign("monthly_billing");
            addParameters(d, "number", "clientName", "departmentName", "invoiceDate",
                    "paymentTerms", "periodDescription", "total", "amountInWords");

            JRDesignBand t = band(390);

            // Letterhead
            t.addElement(text(COMPANY_NAME, 0, 0, 320, 22, 15f, true, HorizontalTextAlignEnum.LEFT));
            t.addElement(text(COMPANY_ADDRESS, 0, 22, 320, 14, 9f, false, HorizontalTextAlignEnum.LEFT));
            t.addElement(text(COMPANY_PHONE, 0, 36, 320, 14, 9f, false, HorizontalTextAlignEnum.LEFT));
            t.addElement(text("INVOICE", 0, 58, CONTENT_WIDTH, 24, 18f, true, HorizontalTextAlignEnum.CENTER));
            t.addElement(rule(0, 88, CONTENT_WIDTH));

            // Hotel (left) + meta (right)
            t.addElement(text("Nama Hotel", 0, 98, 75, 14, 9f, false, HorizontalTextAlignEnum.LEFT));
            t.addElement(paramCell("clientName", 80, 98, 235, 14, 10f, true, HorizontalTextAlignEnum.LEFT));
            t.addElement(paramCell("departmentName", 80, 114, 235, 14, 9f, false, HorizontalTextAlignEnum.LEFT));
            metaRow(t, "Number", "number", 98);
            metaRow(t, "Date", "invoiceDate", 114);
            metaRow(t, "Payment", "paymentTerms", 130);

            // Description table — single period line
            t.addElement(headerCellAt("No", 0, 158, 45, HorizontalTextAlignEnum.CENTER));
            t.addElement(headerCellAt("Description", 45, 158, 385, HorizontalTextAlignEnum.LEFT));
            t.addElement(headerCellAt("Total", 430, 158, 115, HorizontalTextAlignEnum.RIGHT));
            t.addElement(text("1", 0, 180, 45, 16, 9f, false, HorizontalTextAlignEnum.CENTER));
            t.addElement(paramCell("periodDescription", 45, 180, 385, 16, 9f, false, HorizontalTextAlignEnum.LEFT));
            t.addElement(paramCell("total", 430, 180, 115, 16, 9f, false, HorizontalTextAlignEnum.RIGHT));
            t.addElement(rule(0, 202, CONTENT_WIDTH, RULE));

            // Subtotal
            t.addElement(text("SUB TOTAL", 265, 212, 160, 18, 10f, true, HorizontalTextAlignEnum.RIGHT));
            t.addElement(paramCell("total", 430, 212, 115, 18, 10f, true, HorizontalTextAlignEnum.RIGHT));
            t.addElement(rule(0, 234, CONTENT_WIDTH));

            // Terbilang
            t.addElement(text("Terbilang :", 0, 244, 65, 14, 9f, false, HorizontalTextAlignEnum.LEFT));
            t.addElement(paramCell("amountInWords", 68, 244, 477, 16, 9f, true, HorizontalTextAlignEnum.LEFT));

            // Bank transfer details
            t.addElement(text("Please Transfer To", 0, 282, 300, 14, 9f, true, HorizontalTextAlignEnum.LEFT));
            t.addElement(text(BANK_BENEFICIARY, 0, 298, 300, 14, 9f, false, HorizontalTextAlignEnum.LEFT));
            t.addElement(text(BANK_NAME, 0, 312, 300, 14, 9f, false, HorizontalTextAlignEnum.LEFT));
            t.addElement(text("No. Rekening : " + BANK_ACCOUNT, 0, 326, 300, 14, 9f, false, HorizontalTextAlignEnum.LEFT));
            t.addElement(text("Nama Pemilik Rekening : " + BANK_HOLDER, 0, 340, 300, 14, 9f, false, HorizontalTextAlignEnum.LEFT));

            d.setTitle(t);
            return d;
        } catch (JRException e) {
            throw new IllegalStateException("Failed to build monthly billing design", e);
        }
    }

    /**
     * A right-column "label : value" meta line (Number / Date / Payment) on the invoice header.
     */
    private static void metaRow(JRDesignBand band, String label, String paramName, int y) {
        band.addElement(text(label, 320, y, 60, 14, 9f, false, HorizontalTextAlignEnum.LEFT));
        band.addElement(text(":", 382, y, 6, 14, 9f, false, HorizontalTextAlignEnum.LEFT));
        band.addElement(paramCell(paramName, 392, y, 153, 14, 9f, false, HorizontalTextAlignEnum.LEFT));
    }

    // ── design helpers ──

    private static JasperDesign baseDesign(String name) {
        var d = new JasperDesign();
        d.setName(name);
        d.setPageWidth(PAGE_WIDTH);
        d.setPageHeight(842);
        d.setLeftMargin(MARGIN);
        d.setRightMargin(MARGIN);
        d.setTopMargin(MARGIN);
        d.setBottomMargin(MARGIN);
        d.setColumnWidth(CONTENT_WIDTH);
        return d;
    }

    private static void addParameters(JasperDesign d, String... names) throws JRException {
        for (String name : names) {
            var p = new JRDesignParameter();
            p.setName(name);
            p.setValueClass(String.class);
            d.addParameter(p);
        }
    }

    private static void addFields(JasperDesign d, String... names) throws JRException {
        for (String name : names) {
            var f = new JRDesignField();
            f.setName(name);
            f.setValueClass(String.class);
            d.addField(f);
        }
    }

    private static void detailBand(JasperDesign d, JRDesignBand band) {
        ((JRDesignSection) d.getDetailSection()).addBand(band);
    }

    private static JRDesignBand band(int height) {
        var b = new JRDesignBand();
        b.setHeight(height);
        return b;
    }

    /**
     * A label/value pair: bold-ish label on the left, value field beside it.
     */
    private static void labelValue(JRDesignBand band, String label, String paramName, int x, int y, boolean boldValue) {
        band.addElement(text(label, x, y, 80, 16, 9f, false, HorizontalTextAlignEnum.LEFT));
        band.addElement(paramCell(paramName, x + 82, y, 183, 16, 9f, boldValue, HorizontalTextAlignEnum.LEFT));
    }

    private static JRDesignStaticText text(String value, int x, int y, int w, int h, float fontSize,
                                           boolean bold, HorizontalTextAlignEnum align) {
        var t = new JRDesignStaticText();
        t.setText(value);
        place(t, x, y, w, h);
        t.setFontSize(fontSize);
        t.setBold(bold);
        t.setHorizontalTextAlign(align);
        return t;
    }

    private static JRDesignStaticText headerCell(String value, int x, int w, HorizontalTextAlignEnum align) {
        return headerCellAt(value, x, 0, w, align);
    }

    /**
     * Opaque gray header cell at an explicit y (for layouts built inside a single band).
     */
    private static JRDesignStaticText headerCellAt(String value, int x, int y, int w, HorizontalTextAlignEnum align) {
        var t = text(value, x, y, w, 20, 9f, true, align);
        t.setMode(ModeEnum.OPAQUE);
        t.setBackcolor(HEADER_BG);
        return t;
    }

    private static JRDesignTextField fieldCell(String fieldName, int x, int w, HorizontalTextAlignEnum align) {
        return valueCell(chunk(false, fieldName), x, 0, w, 16, 9f, false, align);
    }

    private static JRDesignTextField paramCell(String paramName, int x, int y, int w, int h, float fontSize,
                                               boolean bold, HorizontalTextAlignEnum align) {
        return valueCell(chunk(true, paramName), x, y, w, h, fontSize, bold, align);
    }

    private static JRDesignTextField valueCell(JRDesignExpression expr, int x, int y, int w, int h, float fontSize,
                                               boolean bold, HorizontalTextAlignEnum align) {
        var tf = new JRDesignTextField();
        tf.setExpression(expr);
        tf.setBlankWhenNull(true);
        place(tf, x, y, w, h);
        tf.setFontSize(fontSize);
        tf.setBold(bold);
        tf.setHorizontalTextAlign(align);
        return tf;
    }

    private static JRDesignExpression chunk(boolean parameter, String name) {
        var e = new JRDesignExpression();
        if (parameter) {
            e.addParameterChunk(name);
        } else {
            e.addFieldChunk(name);
        }
        return e;
    }

    private static JRDesignLine rule(int x, int y, int w) {
        return rule(x, y, w, Color.BLACK);
    }

    private static JRDesignLine rule(int x, int y, int w, Color color) {
        var line = new JRDesignLine();
        place(line, x, y, w, 1);
        line.setForecolor(color);
        return line;
    }

    private static void place(net.sf.jasperreports.engine.design.JRDesignElement el, int x, int y, int w, int h) {
        el.setX(x);
        el.setY(y);
        el.setWidth(w);
        el.setHeight(h);
    }
}