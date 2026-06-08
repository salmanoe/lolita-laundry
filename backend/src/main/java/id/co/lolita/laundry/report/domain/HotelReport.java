package id.co.lolita.laundry.report.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * One client's billable activity over a date range: the individual orders and an item breakdown
 * (summed quantity + total per item), plus the range grand total.
 */
public record HotelReport(
        Long clientId,
        String clientName,
        String clientCode,
        LocalDate from,
        LocalDate to,
        List<OrderLine> orders,
        List<ItemLine> items,
        BigDecimal grandTotal) {

    /**
     * A single billable order in the range.
     */
    public record OrderLine(
            Long orderId,
            String orderNumber,
            LocalDate orderDate,
            String status,
            BigDecimal total) {
    }

    /**
     * An item aggregated across the range's orders.
     */
    public record ItemLine(
            String itemName,
            String unit,
            BigDecimal quantity,
            BigDecimal total) {
    }
}
