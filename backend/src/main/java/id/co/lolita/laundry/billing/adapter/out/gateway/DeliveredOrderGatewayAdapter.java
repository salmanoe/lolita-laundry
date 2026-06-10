package id.co.lolita.laundry.billing.adapter.out.gateway;

import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway;
import id.co.lolita.laundry.order.domain.port.in.DeliveredOrderQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Bridges billing's {@link DeliveredOrderGateway} to the order module's
 * {@link DeliveredOrderQuery} (named interface {@code order::api}), mapping order snapshots
 * to billing-owned records so the billing domain stays free of order types.
 */
@Component
@RequiredArgsConstructor
class DeliveredOrderGatewayAdapter implements DeliveredOrderGateway {

    private final DeliveredOrderQuery deliveredOrders;

    @Override
    public Optional<DeliveredOrder> findDeliveredOrder(Long orderId) {
        return deliveredOrders.findDeliveredOrder(orderId).map(DeliveredOrderGatewayAdapter::toDeliveredOrder);
    }

    @Override
    public List<DeliveredOrder> findDeliveredOrders(Long clientId, int year, int month) {
        return deliveredOrders.findDeliveredOrders(clientId, year, month).stream()
                .map(DeliveredOrderGatewayAdapter::toDeliveredOrder)
                .toList();
    }

    @Override
    public Optional<DeliveredOrder> findBillableOrder(Long orderId) {
        return deliveredOrders.findBillableOrder(orderId).map(DeliveredOrderGatewayAdapter::toDeliveredOrder);
    }

    @Override
    public List<DeliveredOrder> findBillableOrders(Long clientId, int year, int month) {
        return deliveredOrders.findBillableOrders(clientId, year, month).stream()
                .map(DeliveredOrderGatewayAdapter::toDeliveredOrder)
                .toList();
    }

    private static DeliveredOrder toDeliveredOrder(DeliveredOrderQuery.DeliveredOrderDetail d) {
        var lines = d.lines().stream()
                .map(l -> new InvoiceLine(l.itemName(), l.unit(), l.quantity(), l.unitPrice(), l.subtotal(),
                        l.departmentId(), l.departmentName()))
                .toList();
        return new DeliveredOrder(d.orderId(), d.orderNumber(), d.clientId(),
                d.orderDate(), d.pricingMultiplier(), d.total(), d.delivered(), lines);
    }
}