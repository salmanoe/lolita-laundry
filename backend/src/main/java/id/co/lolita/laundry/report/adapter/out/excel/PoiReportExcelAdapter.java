package id.co.lolita.laundry.report.adapter.out.excel;

import id.co.lolita.laundry.report.domain.ClientLine;
import id.co.lolita.laundry.report.domain.DailyReport;
import id.co.lolita.laundry.report.domain.HotelReport;
import id.co.lolita.laundry.report.domain.MonthlyReport;
import id.co.lolita.laundry.report.domain.port.out.ReportExcelPort;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Apache POI implementation of {@link ReportExcelPort}. Builds an {@code .xlsx} workbook with a
 * title, a bold/filled header row, data rows, and a bold total row. Money is written as numeric
 * cells with a Rupiah number format so the figures stay sortable/summable in Excel.
 */
@Component
class PoiReportExcelAdapter implements ReportExcelPort {

    private static final String MONEY_FORMAT = "\"Rp\"#,##0";

    private static final String[] MONTHS_ID = {
            "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    @Override
    public byte[] dailyWorkbook(DailyReport r) {
        try (Workbook wb = new XSSFWorkbook()) {
            Styles s = new Styles(wb);
            Sheet sheet = wb.createSheet("Laporan Harian");
            int row = title(sheet, s, 0, "Laporan Harian — " + r.date());
            row++; // blank spacer
            clientTable(sheet, s, row, r.clients(), r.grandTotal());
            autosize(sheet, 4);
            return toBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public byte[] monthlyWorkbook(MonthlyReport r) {
        try (Workbook wb = new XSSFWorkbook()) {
            Styles s = new Styles(wb);
            Sheet sheet = wb.createSheet("Laporan Bulanan");
            int row = title(sheet, s, 0, "Laporan Bulanan — " + MONTHS_ID[r.month()] + " " + r.year());
            row++;
            clientTable(sheet, s, row, r.clients(), r.grandTotal());
            autosize(sheet, 4);
            return toBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public byte[] hotelWorkbook(HotelReport r) {
        try (Workbook wb = new XSSFWorkbook()) {
            Styles s = new Styles(wb);
            Sheet sheet = wb.createSheet("Laporan Per Klien");

            String header = r.clientCode() != null ? r.clientName() + " (" + r.clientCode() + ")" : r.clientName();
            int row = title(sheet, s, 0, "Laporan Per Klien — " + header);
            row = text(sheet, s, row, "Periode: " + r.from() + " s/d " + r.to());
            row = text(sheet, s, row, r.orders().size() + " order");
            row++; // blank spacer

            // ── Item breakdown ──
            row = text(sheet, s, row, "Rincian Item", s.bold);
            Row ih = sheet.createRow(row++);
            cell(ih, 0, "Item", s.tableHeader);
            cell(ih, 1, "Satuan", s.tableHeader);
            cell(ih, 2, "Qty", s.tableHeader);
            cell(ih, 3, "Total", s.tableHeader);
            for (HotelReport.ItemLine i : r.items()) {
                Row dr = sheet.createRow(row++);
                cell(dr, 0, i.itemName(), s.normal);
                cell(dr, 1, i.unit(), s.normal);
                numCell(dr, 2, i.quantity(), s.normal);
                moneyCell(dr, 3, i.total(), s.money);
            }
            row++; // blank spacer

            // ── Orders ──
            row = text(sheet, s, row, "Daftar Order", s.bold);
            Row oh = sheet.createRow(row++);
            cell(oh, 0, "No. Order", s.tableHeader);
            cell(oh, 1, "Tanggal", s.tableHeader);
            cell(oh, 2, "Status", s.tableHeader);
            cell(oh, 3, "Total", s.tableHeader);
            for (HotelReport.OrderLine o : r.orders()) {
                Row dr = sheet.createRow(row++);
                cell(dr, 0, o.orderNumber(), s.normal);
                cell(dr, 1, String.valueOf(o.orderDate()), s.normal);
                cell(dr, 2, statusLabel(o.status()), s.normal);
                moneyCell(dr, 3, o.total(), s.money);
            }
            Row totalRow = sheet.createRow(row);
            cell(totalRow, 0, "Total", s.bold);
            moneyCell(totalRow, 3, r.grandTotal(), s.boldMoney);

            autosize(sheet, 4);
            return toBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ── Shared table for daily + monthly (per-client totals) ──
    private static void clientTable(Sheet sheet, Styles s, int startRow, List<ClientLine> clients, BigDecimal grandTotal) {
        int r = startRow;
        Row header = sheet.createRow(r++);
        cell(header, 0, "Klien", s.tableHeader);
        cell(header, 1, "Kode", s.tableHeader);
        cell(header, 2, "Jml Order", s.tableHeader);
        cell(header, 3, "Total", s.tableHeader);

        for (ClientLine c : clients) {
            Row row = sheet.createRow(r++);
            cell(row, 0, c.clientName(), s.normal);
            cell(row, 1, c.clientCode(), s.normal);
            numCell(row, 2, BigDecimal.valueOf(c.orderCount()), s.normal);
            moneyCell(row, 3, c.total(), s.money);
        }

        Row total = sheet.createRow(r);
        cell(total, 0, "Total", s.bold);
        numCell(total, 2, BigDecimal.valueOf(clients.stream().mapToLong(ClientLine::orderCount).sum()), s.bold);
        moneyCell(total, 3, grandTotal, s.boldMoney);
    }

    // ── Cell + row helpers ──
    private static int title(Sheet sheet, Styles s, int r, String value) {
        cell(sheet.createRow(r), 0, value, s.title);
        return r + 1;
    }

    private static int text(Sheet sheet, Styles s, int r, String value) {
        return text(sheet, s, r, value, s.normal);
    }

    private static int text(Sheet sheet, Styles s, int r, String value, CellStyle style) {
        cell(sheet.createRow(r), 0, value, style);
        return r + 1;
    }

    private static void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(sanitize(value));
        c.setCellStyle(style);
    }

    /**
     * Neutralizes CSV/Excel formula (and DDE) injection: client names, item names, and order
     * numbers are staff-editable free text that flows unescaped into text cells. A value renamed to
     * e.g. {@code =cmd|'/c calc'!A1} becomes a live formula the moment a user opens the export in
     * Excel. Prefixing any leading {@code = + - @}, tab, or CR with a single quote forces Excel to
     * treat the cell as literal text — the standard mitigation, applied once here so every current
     * and future call site is covered. Numeric cells ({@code numCell}/{@code moneyCell}) are unaffected.
     */
    private static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            return "'" + value;
        }
        return value;
    }

    private static void numCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? 0 : value.doubleValue());
        c.setCellStyle(style);
    }

    private static void moneyCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? 0 : value.doubleValue());
        c.setCellStyle(style);
    }

    private static void autosize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static byte[] toBytes(Workbook wb) throws IOException {
        var out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "RECEIVED" -> "Diterima";
            case "PROCESSING" -> "Diproses";
            case "DONE" -> "Selesai";
            case "DELIVERED" -> "Terkirim";
            case "CANCELLED" -> "Dibatalkan";
            default -> status;
        };
    }

    /**
     * Workbook-scoped cell styles (POI styles cannot be shared across workbooks).
     */
    private static final class Styles {
        final CellStyle title;
        final CellStyle tableHeader;
        final CellStyle normal;
        final CellStyle bold;
        final CellStyle money;
        final CellStyle boldMoney;

        Styles(Workbook wb) {
            Font boldFont = wb.createFont();
            boldFont.setBold(true);

            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            short moneyFormat = wb.createDataFormat().getFormat(MONEY_FORMAT);

            title = wb.createCellStyle();
            title.setFont(titleFont);

            tableHeader = wb.createCellStyle();
            tableHeader.setFont(boldFont);
            tableHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            tableHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            normal = wb.createCellStyle();

            bold = wb.createCellStyle();
            bold.setFont(boldFont);

            money = wb.createCellStyle();
            money.setDataFormat(moneyFormat);

            boldMoney = wb.createCellStyle();
            boldMoney.setFont(boldFont);
            boldMoney.setDataFormat(moneyFormat);
        }
    }
}