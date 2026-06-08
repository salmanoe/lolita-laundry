package id.co.lolita.laundry.report.domain.port.in;

import id.co.lolita.laundry.report.domain.DailyReport;
import id.co.lolita.laundry.report.domain.HotelReport;
import id.co.lolita.laundry.report.domain.MonthlyReport;

import java.time.LocalDate;

/**
 * Inbound port: the Phase 4 reports (daily summary, monthly per-client, per-hotel range).
 */
public interface GetReportsUseCase {

    DailyReport daily(LocalDate date);

    MonthlyReport monthly(int year, int month);

    HotelReport hotel(Long clientId, LocalDate from, LocalDate to);
}