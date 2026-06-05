package id.co.lolita.laundry.order.domain.port.in;

import id.co.lolita.laundry.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A driver's view of the orders assigned to them. Deliberately price-free: drivers see
 * what to deliver (items, quantities, where, when) but never prices, subtotals, or the
 * pricing multiplier.
 */
public interface GetDriverDeliveriesUseCase {

    /**
     * One assigned order as the driver sees it — no monetary fields.
     */
    record DriverDeliveryView(
            Long orderId,
            String orderNumber,
            String clientName,
            String departmentName,   // null unless the client bills per department
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
     * Open assignments for the driver (not yet delivered), ready ones first.
     */
    List<DriverDeliveryView> getAssignedDeliveries(Long driverId);
}