package id.co.lolita.laundry.report.domain.port.out;

import id.co.lolita.laundry.report.domain.DailyReport;
import id.co.lolita.laundry.report.domain.HotelReport;
import id.co.lolita.laundry.report.domain.MonthlyReport;

/**
 * Renders a report into an {@code .xlsx} workbook (bytes). Outbound port — the Apache POI
 * implementation lives in {@code adapter/out/excel}, keeping POI out of the domain/application.
 */
public interface ReportExcelPort {

    byte[] dailyWorkbook(DailyReport report);

    byte[] monthlyWorkbook(MonthlyReport report);

    byte[] hotelWorkbook(HotelReport report);
}