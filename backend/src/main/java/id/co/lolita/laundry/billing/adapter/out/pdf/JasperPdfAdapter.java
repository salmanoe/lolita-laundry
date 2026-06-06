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

    private volatile JasperReport orderInvoiceReport;
    private volatile JasperReport monthlyBillingReport;

    // ── InvoicePdfPort ──

    @Override
    public byte[] renderOrderInvoice(OrderInvoiceDocument doc) {
        Map<String, Object> params = new HashMap<>();
        params.put("invoiceNumber", doc.invoiceNumber());
        params.put("invoiceDate", doc.invoiceDate());
        params.put("clientLine", clientLine(doc.clientName(), doc.clientCode()));
        params.put("orderNumber", doc.orderNumber());
        params.put("orderDate", doc.orderDate());
        params.put("orderTypeLabel", doc.orderTypeLabel());
        params.put("departmentName", doc.departmentName());
        params.put("total", doc.total());

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

    @Override
    public byte[] renderMonthlyBilling(MonthlyBillingDocument doc) {
        Map<String, Object> params = new HashMap<>();
        params.put("billingNumber", doc.billingNumber());
        params.put("periodLabel", doc.periodLabel());
        params.put("clientLine", clientLine(doc.clientName(), doc.clientCode()));
        params.put("departmentName", doc.departmentName());
        params.put("invoiceDate", doc.invoiceDate());
        params.put("total", doc.total());

        List<Map<String, ?>> rows = new ArrayList<>();
        for (BillingOrderRow order : doc.orders()) {
            Map<String, Object> row = new HashMap<>();
            row.put("orderNumber", order.orderNumber());
            row.put("orderDate", order.orderDate());
            row.put("subtotal", order.subtotal());
            rows.add(row);
        }
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

            JRDesignBand title = band(150);
            title.addElement(text("Lolita Laundry", 0, 0, 300, 26, 16f, true, HorizontalTextAlignEnum.LEFT));
            title.addElement(text("INVOICE", CONTENT_WIDTH - 200, 0, 200, 26, 16f, true, HorizontalTextAlignEnum.RIGHT));
            title.addElement(rule(0, 32, CONTENT_WIDTH));
            labelValue(title, "No. Invoice", "invoiceNumber", 0, 46, true);
            labelValue(title, "Tanggal", "invoiceDate", 0, 64, false);
            labelValue(title, "Jenis", "orderTypeLabel", 0, 82, false);
            labelValue(title, "Klien", "clientLine", 280, 46, true);
            labelValue(title, "Departemen", "departmentName", 280, 64, false);
            labelValue(title, "No. Order", "orderNumber", 280, 82, false);
            labelValue(title, "Tgl Order", "orderDate", 280, 100, false);
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

    private JasperDesign buildMonthlyBillingDesign() {
        try {
            JasperDesign d = baseDesign("monthly_billing");
            addParameters(d, "billingNumber", "periodLabel", "clientLine", "departmentName", "invoiceDate", "total");
            addFields(d, "orderNumber", "orderDate", "subtotal");

            JRDesignBand title = band(140);
            title.addElement(text("Lolita Laundry", 0, 0, 300, 26, 16f, true, HorizontalTextAlignEnum.LEFT));
            title.addElement(text("TAGIHAN BULANAN", CONTENT_WIDTH - 250, 0, 250, 26, 14f, true, HorizontalTextAlignEnum.RIGHT));
            title.addElement(rule(0, 32, CONTENT_WIDTH));
            labelValue(title, "No. Tagihan", "billingNumber", 0, 46, true);
            labelValue(title, "Periode", "periodLabel", 0, 64, true);
            labelValue(title, "Tgl Terbit", "invoiceDate", 0, 82, false);
            labelValue(title, "Klien", "clientLine", 280, 46, true);
            labelValue(title, "Departemen", "departmentName", 280, 64, false);
            d.setTitle(title);

            JRDesignBand head = band(22);
            head.addElement(headerCell("No. Order", 0, 300, HorizontalTextAlignEnum.LEFT));
            head.addElement(headerCell("Tgl Order", 300, 130, HorizontalTextAlignEnum.LEFT));
            head.addElement(headerCell("Subtotal", 430, 115, HorizontalTextAlignEnum.RIGHT));
            d.setColumnHeader(head);

            JRDesignBand detail = band(18);
            detail.addElement(fieldCell("orderNumber", 0, 300, HorizontalTextAlignEnum.LEFT));
            detail.addElement(fieldCell("orderDate", 300, 130, HorizontalTextAlignEnum.LEFT));
            detail.addElement(fieldCell("subtotal", 430, 115, HorizontalTextAlignEnum.RIGHT));
            detail.addElement(rule(0, 17, CONTENT_WIDTH, RULE));
            detailBand(d, detail);

            JRDesignBand summary = band(50);
            summary.addElement(rule(0, 2, CONTENT_WIDTH));
            summary.addElement(text("GRAND TOTAL", 265, 12, 160, 22, 13f, true, HorizontalTextAlignEnum.RIGHT));
            summary.addElement(paramCell("total", 430, 12, 115, 22, 13f, true, HorizontalTextAlignEnum.RIGHT));
            d.setSummary(summary);
            return d;
        } catch (JRException e) {
            throw new IllegalStateException("Failed to build monthly billing design", e);
        }
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

    /** A label/value pair: bold-ish label on the left, value field beside it. */
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
        var t = text(value, x, 0, w, 20, 9f, true, align);
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