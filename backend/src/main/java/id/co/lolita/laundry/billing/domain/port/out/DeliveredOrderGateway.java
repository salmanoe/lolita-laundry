package id.co.lolita.laundry.billing.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Billing's view of delivered orders. The adapter delegates to the order module's
 * {@code DeliveredOrderQuery} (named interface {@code order::api}) and maps its snapshots to
 * these billing-owned records, so the billing domain never references order types.
 */
public interface DeliveredOrderGateway {

    record InvoiceLine(String itemName, String unit, BigDecimal quantity,
                       BigDecimal unitPrice, BigDecimal subtotal,
                       Long departmentId, String departmentName) {
    }

    record DeliveredOrder(Long orderId, String orderNumber, Long clientId,
                          LocalDate orderDate, BigDecimal pricingMultiplier,
                          BigDecimal total, boolean delivered, List<InvoiceLine> lines) {
    }

    Optional<DeliveredOrder> findDeliveredOrder(Long orderId);

    List<DeliveredOrder> findDeliveredOrders(Long clientId, int year, int month);

    /**
     * A single billable (not canceled) order, or empty if unknown/canceled.
     */
    Optional<DeliveredOrder> findBillableOrder(Long orderId);

    /**
     * Every billable (not canceled) order for a client in the given month, oldest first.
     */
    List<DeliveredOrder> findBillableOrders(Long clientId, int year, int month);
}