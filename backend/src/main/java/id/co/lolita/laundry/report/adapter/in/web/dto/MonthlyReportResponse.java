package id.co.lolita.laundry.report.adapter.in.web.dto;

import id.co.lolita.laundry.report.domain.MonthlyReport;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyReportResponse(
        int year,
        int month,
        List<ClientLineResponse> clients,
        BigDecimal grandTotal) {

    public static MonthlyReportResponse from(MonthlyReport r) {
        return new MonthlyReportResponse(r.year(), r.month(),
                r.clients().stream().map(ClientLineResponse::from).toList(),
                r.grandTotal());
    }
}