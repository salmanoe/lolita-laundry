package id.co.lolita.laundry.billing.adapter.in.web.dto;

import id.co.lolita.laundry.billing.domain.MonthlyBillingLine;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyBillingLineResponse(Long orderId, String orderNumber, LocalDate orderDate, BigDecimal subtotal) {

    public static MonthlyBillingLineResponse from(MonthlyBillingLine l) {
        return new MonthlyBillingLineResponse(l.orderId(), l.orderNumber(), l.orderDate(), l.subtotal());
    }
}