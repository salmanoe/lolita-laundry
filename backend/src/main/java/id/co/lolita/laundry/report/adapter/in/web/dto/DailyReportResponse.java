package id.co.lolita.laundry.report.adapter.in.web.dto;

import id.co.lolita.laundry.report.domain.DailyReport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailyReportResponse(
        LocalDate date,
        List<ClientLineResponse> clients,
        BigDecimal grandTotal) {

    public static DailyReportResponse from(DailyReport r) {
        return new DailyReportResponse(r.date(),
                r.clients().stream().map(ClientLineResponse::from).toList(),
                r.grandTotal());
    }
}