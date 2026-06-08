package id.co.lolita.laundry.report.adapter.in.web.dto;

import id.co.lolita.laundry.report.domain.HotelReport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HotelReportResponse(
        Long clientId,
        String clientName,
        String clientCode,
        LocalDate from,
        LocalDate to,
        List<OrderLineResponse> orders,
        List<ItemLineResponse> items,
        BigDecimal grandTotal) {

    public record OrderLineResponse(
            Long orderId,
            String orderNumber,
            LocalDate orderDate,
            String status,
            BigDecimal total) {
    }

    public record ItemLineResponse(
            String itemName,
            String unit,
            BigDecimal quantity,
            BigDecimal total) {
    }

    public static HotelReportResponse from(HotelReport r) {
        return new HotelReportResponse(
                r.clientId(), r.clientName(), r.clientCode(), r.from(), r.to(),
                r.orders().stream()
                        .map(o -> new OrderLineResponse(o.orderId(), o.orderNumber(), o.orderDate(),
                                o.status(), o.total()))
                        .toList(),
                r.items().stream()
                        .map(i -> new ItemLineResponse(i.itemName(), i.unit(), i.quantity(), i.total()))
                        .toList(),
                r.grandTotal());
    }
}