package id.co.lolita.laundry.report.domain.port.in;

import id.co.lolita.laundry.report.domain.DashboardSummary;

/**
 * Inbound port: build the owner/staff dashboard summary.
 */
public interface GetDashboardUseCase {

    DashboardSummary dashboard();
}