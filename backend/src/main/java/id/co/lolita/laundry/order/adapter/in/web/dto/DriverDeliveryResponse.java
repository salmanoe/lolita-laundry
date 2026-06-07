package id.co.lolita.laundry.order.adapter.in.web.dto;

import id.co.lolita.laundry.order.domain.OrderStatus;
import id.co.lolita.laundry.order.domain.port.in.GetDriverDeliveriesUseCase.DriverDeliveryView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * An open-pool order as a driver sees it. Price-free by design — no unit price, subtotal,
 * total, or pricing multiplier is ever sent to a driver.
 */
public record DriverDeliveryResponse(
        Long orderId,
        String orderNumber,
        String clientName,
        LocalDate orderDate,
        LocalDate dueDate,
        OrderStatus status,
        String notes,
        List<Line> lines
) {
    public record Line(String itemName, String unitName, BigDecimal quantity) {
    }

    public static DriverDeliveryResponse from(DriverDeliveryView v) {
        return new DriverDeliveryResponse(
                v.orderId(), v.orderNumber(), v.clientName(),
                v.orderDate(), v.dueDate(), v.status(), v.notes(),
                v.lines().stream().map(l -> new Line(l.itemName(), l.unitName(), l.quantity())).toList()
        );
    }
}
