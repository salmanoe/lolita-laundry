package id.co.lolita.laundry.order.domain.port.in;

import id.co.lolita.laundry.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The shared open delivery pool as a driver sees it. Every driver sees every order not yet
 * delivered — there is no per-driver assignment; a driver picks what to deliver and confirms
 * it. Deliberately price-free: drivers see what to deliver (items, quantities, where, when)
 * but never prices, subtotals, or the pricing multiplier.
 */
public interface GetDriverDeliveriesUseCase {

    /**
     * One open order as the driver sees it — no monetary fields.
     */
    record DriverDeliveryView(
            Long orderId,
            String orderNumber,
            String clientName,
            LocalDate orderDate,
            LocalDate dueDate,
            OrderStatus status,
            String notes,
            List<DriverLine> lines
    ) {
    }

    /**
     * A line item without any pricing — just what and how much.
     */
    record DriverLine(String itemName, String unitName, BigDecimal quantity) {
    }

    /**
     * The open delivery pool — every order not yet delivered, ready ones first.
     */
    List<DriverDeliveryView> getOpenDeliveries();
}