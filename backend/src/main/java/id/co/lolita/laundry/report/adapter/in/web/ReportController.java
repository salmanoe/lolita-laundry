package id.co.lolita.laundry.report.adapter.in.web;

import id.co.lolita.laundry.report.adapter.in.web.dto.DailyReportResponse;
import id.co.lolita.laundry.report.adapter.in.web.dto.HotelReportResponse;
import id.co.lolita.laundry.report.adapter.in.web.dto.MonthlyReportResponse;
import id.co.lolita.laundry.report.domain.port.in.ExportReportsUseCase;
import id.co.lolita.laundry.report.domain.port.in.ExportReportsUseCase.ExcelFile;
import id.co.lolita.laundry.report.domain.port.in.GetReportsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Reports for Lolita staff (OWNER / STAFF): daily summary, monthly per-client, per-hotel range.
 * Money is "billable orders by order date" — every non-canceled order, multiplier-inclusive.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FINANCE_STAFF', 'SUPER_ADMIN')")
class ReportController {

    private final GetReportsUseCase reports;
    private final ExportReportsUseCase exports;

    @GetMapping("/daily")
    DailyReportResponse daily(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return DailyReportResponse.from(reports.daily(date != null ? date : LocalDate.now()));
    }

    @GetMapping("/monthly")
    MonthlyReportResponse monthly(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        YearMonth ym = (year != null && month != null) ? YearMonth.of(year, month) : YearMonth.now();
        return MonthlyReportResponse.from(reports.monthly(ym.getYear(), ym.getMonthValue()));
    }

    @GetMapping("/hotel/{id}")
    HotelReportResponse hotel(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return HotelReportResponse.from(reports.hotel(id, from, to));
    }

    // ── Excel (.xlsx) exports (Phase 5) — same OWNER/STAFF gate as the JSON reports above ──

    @GetMapping("/daily/export")
    ResponseEntity<byte[]> dailyExport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return xlsx(exports.dailyExcel(date != null ? date : LocalDate.now()));
    }

    @GetMapping("/monthly/export")
    ResponseEntity<byte[]> monthlyExport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        YearMonth ym = (year != null && month != null) ? YearMonth.of(year, month) : YearMonth.now();
        return xlsx(exports.monthlyExcel(ym.getYear(), ym.getMonthValue()));
    }

    @GetMapping("/hotel/{id}/export")
    ResponseEntity<byte[]> hotelExport(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return xlsx(exports.hotelExcel(id, from, to));
    }

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private static ResponseEntity<byte[]> xlsx(ExcelFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename()).build().toString())
                .contentType(XLSX)
                .body(file.content());
    }
}