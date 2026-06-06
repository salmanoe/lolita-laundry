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
     */
    record DeliveredLine(String itemName, String unit, BigDecimal quantity,
                         BigDecimal unitPrice, BigDecimal subtotal) {
    }

    /**
     * Everything billing needs about one delivered order.
     *
     * @param departmentId   nullable — only set for PER_DEPARTMENT clients (e.g. PBS)
     * @param departmentName nullable — resolved display name for {@code departmentId}
     * @param total          sum of line subtotals (already includes the pricing multiplier)
     */
    record DeliveredOrderDetail(Long orderId, String orderNumber, Long clientId, Long departmentId,
                                String departmentName, LocalDate orderDate, BigDecimal pricingMultiplier,
                                BigDecimal total, List<DeliveredLine> lines) {
    }

    /** A single delivered order, or empty if the order is unknown or not yet DELIVERED. */
    Optional<DeliveredOrderDetail> findDeliveredOrder(Long orderId);

    /**
     * Every DELIVERED order for a client whose order date falls in the given month.
     * Ordered by order date ascending. Empty if none.
     */
    List<DeliveredOrderDetail> findDeliveredOrders(Long clientId, int year, int month);
}