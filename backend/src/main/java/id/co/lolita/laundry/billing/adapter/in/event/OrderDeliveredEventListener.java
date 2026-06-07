package id.co.lolita.laundry.billing.adapter.in.event;

import id.co.lolita.laundry.billing.domain.port.in.CreateOrderInvoiceUseCase;
import id.co.lolita.laundry.order.domain.event.OrderDeliveredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Billing's reaction to an order being delivered: generate the per-order invoice. Inbound
 * adapter — depends only on the {@link CreateOrderInvoiceUseCase} inbound port.
 *
 * <p>{@link ApplicationModuleListener} = async + a new transaction + after-commit. Combined
 * with the Modulith JPA event registry, the publication is persisted and retried until the
 * listener completes, so a delivered order always ends up invoiced even across a crash.
 */
@Component
@RequiredArgsConstructor
class OrderDeliveredEventListener {

    private final CreateOrderInvoiceUseCase createOrderInvoice;

    @ApplicationModuleListener
    void on(OrderDeliveredEvent event) {
        createOrderInvoice.createForDeliveredOrder(event.orderId());
    }
}