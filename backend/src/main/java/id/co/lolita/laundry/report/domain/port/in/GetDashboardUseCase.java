package id.co.lolita.laundry.report.domain.port.in;

import id.co.lolita.laundry.report.domain.DashboardAnalytics;
import id.co.lolita.laundry.report.domain.DashboardSummary;

import java.time.LocalDate;

/**
 * Inbound port: build the dashboards. {@link #dashboard()} is the operational summary (OWNER/STAFF);
 * {@link #analytics(LocalDate, LocalDate)} is the owner-only business-analytics view over a range.
 */
public interface GetDashboardUseCase {

    DashboardSummary dashboard();

    DashboardAnalytics analytics(LocalDate from, LocalDate to);
}