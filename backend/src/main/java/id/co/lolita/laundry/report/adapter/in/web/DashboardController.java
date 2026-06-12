package id.co.lolita.laundry.report.adapter.in.web;

import id.co.lolita.laundry.report.adapter.in.web.dto.DashboardAnalyticsResponse;
import id.co.lolita.laundry.report.adapter.in.web.dto.DashboardSummaryResponse;
import id.co.lolita.laundry.report.adapter.in.web.dto.FinanceTrendResponse;
import id.co.lolita.laundry.report.domain.port.in.GetDashboardUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard endpoints. The operational {@code /summary} is FINANCE_STAFF/SUPER_ADMIN; the
 * business-analytics {@code /analytics} is SUPER_ADMIN only. SUPER_ADMIN sees both; FINANCE_STAFF
 * sees only the operational summary. DAILY_STAFF has no dashboard access.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FINANCE_STAFF', 'SUPER_ADMIN')")
class DashboardController {

    private final GetDashboardUseCase dashboard;

    // Operational summary — FINANCE_STAFF + SUPER_ADMIN.
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('FINANCE_STAFF', 'SUPER_ADMIN')")
    DashboardSummaryResponse summary() {
        return DashboardSummaryResponse.from(dashboard.dashboard());
    }

    // Business analytics over a date range — SUPER_ADMIN only (the old OWNER power). FINANCE_STAFF get 403.
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    DashboardAnalyticsResponse analytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return DashboardAnalyticsResponse.from(dashboard.analytics(from, to));
    }

    // Monthly revenue + order-count series for the FINANCE_STAFF dashboard trend (inherits the
    // class FINANCE_STAFF/SUPER_ADMIN gate — no business-analytics detail, just totals per month).
    @GetMapping("/finance-trend")
    List<FinanceTrendResponse> financeTrend(@RequestParam(defaultValue = "6") int months) {
        return dashboard.financeTrend(months).stream().map(FinanceTrendResponse::from).toList();
    }
}