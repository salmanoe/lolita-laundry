package id.co.lolita.laundry.report.adapter.in.web;

import id.co.lolita.laundry.report.adapter.in.web.dto.DashboardSummaryResponse;
import id.co.lolita.laundry.report.domain.port.in.GetDashboardUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard summary for Lolita staff (OWNER / STAFF). The driver app has no access.
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
}