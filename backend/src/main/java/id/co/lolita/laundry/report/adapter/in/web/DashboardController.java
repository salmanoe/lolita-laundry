package id.co.lolita.laundry.report.adapter.in.web;

import id.co.lolita.laundry.report.adapter.in.web.dto.DashboardAnalyticsResponse;
import id.co.lolita.laundry.report.adapter.in.web.dto.DashboardSummaryResponse;
import id.co.lolita.laundry.report.domain.port.in.GetDashboardUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Dashboard endpoints. The operational {@code /summary} is OWNER/STAFF; the business-analytics
 * {@code /analytics} is OWNER-only. The driver app has no access to either.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
class DashboardController {

    private final GetDashboardUseCase dashboard;

    @GetMapping("/summary")
    DashboardSummaryResponse summary() {
        return DashboardSummaryResponse.from(dashboard.dashboard());
    }

    // Owner-grade business analytics over a date range. Method-level @PreAuthorize overrides the
    // class-level OWNER/STAFF — STAFF get 403.
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('OWNER')")
    DashboardAnalyticsResponse analytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return DashboardAnalyticsResponse.from(dashboard.analytics(from, to));
    }
}