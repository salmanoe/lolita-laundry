package id.co.lolita.laundry.report.domain.port.in;

import java.time.LocalDate;

/**
 * Inbound port: export a report as an Excel ({@code .xlsx}) download. Returns the file bytes plus
 * a suggested filename (built in the application layer); the web adapter sets the HTTP headers.
 */
public interface ExportReportsUseCase {

    ExcelFile dailyExcel(LocalDate date);

    ExcelFile monthlyExcel(int year, int month);

    ExcelFile hotelExcel(Long clientId, LocalDate from, LocalDate to);

    record ExcelFile(String filename, byte[] content) {
    }
}