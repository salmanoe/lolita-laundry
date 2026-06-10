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
 * Dashboard endpoints. The operational {@code /summary} is STAFF/SUPER_ADMIN; the business-analytics
 * {@code /analytics} is OWNER/SUPER_ADMIN. SUPER_ADMIN sees both; OWNER sees only analytics and STAFF
 * only the operational summary. The driver app has no access to either.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'STAFF', 'SUPER_ADMIN')")
class DashboardController {

    private final GetDashboardUseCase dashboard;

    // Operational summary — STAFF + SUPER_ADMIN. Method-level @PreAuthorize overrides the class
    // rule, so OWNER (analytics-only) gets 403 here.
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('STAFF', 'SUPER_ADMIN')")
    DashboardSummaryResponse summary() {
        return DashboardSummaryResponse.from(dashboard.dashboard());
    }

    // Business analytics over a date range — OWNER + SUPER_ADMIN. STAFF get 403.
    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('OWNER', 'SUPER_ADMIN')")
    DashboardAnalyticsResponse analytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return DashboardAnalyticsResponse.from(dashboard.analytics(from, to));
    }
}