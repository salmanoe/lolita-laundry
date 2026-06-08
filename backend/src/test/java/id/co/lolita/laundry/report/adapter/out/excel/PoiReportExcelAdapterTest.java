package id.co.lolita.laundry.report.adapter.out.excel;

import id.co.lolita.laundry.report.domain.ClientLine;
import id.co.lolita.laundry.report.domain.DailyReport;
import id.co.lolita.laundry.report.domain.HotelReport;
import id.co.lolita.laundry.report.domain.MonthlyReport;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real Apache POI rendering (no Spring) — asserts each workbook is a valid,
 * non-empty .xlsx with the expected sheet, and that the title/data round-trips. Also catches
 * headless/runtime POI issues (e.g. autoSizeColumn) without needing a running app.
 */
class PoiReportExcelAdapterTest {

    private final PoiReportExcelAdapter adapter = new PoiReportExcelAdapter();

    @Test
    void dailyWorkbookIsValidXlsxWithData() throws Exception {
        var report = new DailyReport(LocalDate.of(2026, 6, 8),
                List.of(new ClientLine(1L, "Alpha Hotel", "ALP", 3, new BigDecimal("300000"))),
                new BigDecimal("300000"));

        byte[] bytes = adapter.dailyWorkbook(report);

        assertThat(bytes).startsWith((byte) 'P', (byte) 'K'); // ZIP/xlsx magic
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Laporan Harian");
            assertThat(cellTextSomewhere(sheet, "Alpha Hotel")).isTrue();
            assertThat(cellTextSomewhere(sheet, "Total")).isTrue();
        }
    }

    @Test
    void monthlyWorkbookUsesIndonesianMonthName() throws Exception {
        var report = new MonthlyReport(2026, 6,
                List.of(new ClientLine(1L, "PBS", "PBS", 4, new BigDecimal("900000"))),
                new BigDecimal("900000"));

        byte[] bytes = adapter.monthlyWorkbook(report);

        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Laporan Bulanan");
            assertThat(cellTextSomewhere(sheet, "Laporan Bulanan — Juni 2026")).isTrue();
        }
    }

    @Test
    void hotelWorkbookHasItemAndOrderSections() throws Exception {
        var report = new HotelReport(7L, "Frances", "FRA",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                List.of(new HotelReport.OrderLine(10L, "FRA-20260601-001", LocalDate.of(2026, 6, 1),
                        "DELIVERED", new BigDecimal("120000"))),
                List.of(new HotelReport.ItemLine("Towel", "pcs", new BigDecimal("10"), new BigDecimal("50000"))),
                new BigDecimal("120000"));

        byte[] bytes = adapter.hotelWorkbook(report);

        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(cellTextSomewhere(sheet, "Rincian Item")).isTrue();
            assertThat(cellTextSomewhere(sheet, "Daftar Order")).isTrue();
            assertThat(cellTextSomewhere(sheet, "FRA-20260601-001")).isTrue();
            assertThat(cellTextSomewhere(sheet, "Terkirim")).isTrue(); // DELIVERED → Indonesian label
        }
    }

    private static boolean cellTextSomewhere(Sheet sheet, String text) {
        for (var row : sheet) {
            for (var cell : row) {
                if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                        && text.equals(cell.getStringCellValue())) {
                    return true;
                }
            }
        }
        return false;
    }
}
