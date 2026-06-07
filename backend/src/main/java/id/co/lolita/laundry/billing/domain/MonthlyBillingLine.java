package id.co.lolita.laundry.billing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One delivered order within a monthly billing. Order number and date are denormalized so
 * the billing reads stand-alone (it is the payment document of record). Item-level detail
 * lives on each order's own Order Invoice — the monthly billing aggregates at order level.
 */
public record MonthlyBillingLine(Long id, Long orderId, String orderNumber, LocalDate orderDate,
                                 BigDecimal subtotal) {

    public static MonthlyBillingLine of(Long orderId, String orderNumber, LocalDate orderDate, BigDecimal subtotal) {
        return new MonthlyBillingLine(null, orderId, orderNumber, orderDate, subtotal);
    }
}