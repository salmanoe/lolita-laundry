package id.co.lolita.laundry.report.adapter.in.web.dto;

import id.co.lolita.laundry.report.domain.ClientLine;

import java.math.BigDecimal;

public record ClientLineResponse(
        Long clientId,
        String clientName,
        String clientCode,
        long orderCount,
        BigDecimal total) {

    public static ClientLineResponse from(ClientLine l) {
        return new ClientLineResponse(l.clientId(), l.clientName(), l.clientCode(),
                l.orderCount(), l.total());
    }
}