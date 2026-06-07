package id.co.lolita.laundry.order.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Read-only lookups of delivered orders that the {@code billing} module needs to produce
 * Order Invoices (per order) and Monthly Billings (aggregated per client per month).
 *
 * <p>Exposed cross-module (named interface "api"). Item names, units and the department
 * name are resolved inside the order module so the returned snapshots are self-contained —
 * no order, catalog or client domain types leak across the boundary.
 */
public interface DeliveredOrderQuery {

    /**
     * A priced line on a delivered order, with the item already resolved to its display name.
     * {@code departmentId}/{@code departmentName} are set for PER_DEPARTMENT clients (null for
     * COMBINED) — billing splits the order across departments on these.
     */
    record DeliveredLine(String itemName, String unit, BigDecimal quantity,
                         BigDecimal unitPrice, BigDecimal subtotal,
                         Long departmentId, String departmentName) {
    }

    /**
     * Everything billing needs about one delivered order. An order may span several departments
     * (each line carries its own); the monthly billing splits it per department.
     *
     * @param total sum of line subtotals (already includes the pricing multiplier)
     */
    record DeliveredOrderDetail(Long orderId, String orderNumber, Long clientId,
                                LocalDate orderDate, BigDecimal pricingMultiplier,
                                BigDecimal total, List<DeliveredLine> lines) {
    }

    /**
     * A single delivered order, or empty if the order is unknown or not yet DELIVERED.
     */
    Optional<DeliveredOrderDetail> findDeliveredOrder(Long orderId);

    /**
     * Every DELIVERED order for a client whose order date falls in the given month.
     * Ordered by order date ascending. Empty if none.
     */
    List<DeliveredOrderDetail> findDeliveredOrders(Long clientId, int year, int month);

    /**
     * A single <em>billable</em> order (any status except CANCELLED), or empty if the order is
     * unknown or canceled. Drives the auto-built monthly billing: present → on the bill,
     * empty → removed.
     */
    Optional<DeliveredOrderDetail> findBillableOrder(Long orderId);

    /**
     * Every billable (not CANCELLED) order for a client whose order date falls in the given
     * month, oldest first. Backs the manual billing rebuild.
     */
    List<DeliveredOrderDetail> findBillableOrders(Long clientId, int year, int month);
}