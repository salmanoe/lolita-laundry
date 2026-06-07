package id.co.lolita.laundry.billing.adapter.in.event;

import id.co.lolita.laundry.billing.domain.port.in.SyncOrderBillingUseCase;
import id.co.lolita.laundry.order.domain.event.OrderBillingSyncEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Billing's reaction to an order's billable state changing (created / edited / canceled):
 * upsert it into — or remove it from — the client's monthly DRAFT billing. Inbound adapter,
 * depends only on {@link SyncOrderBillingUseCase}.
 *
 * <p>{@link ApplicationModuleListener} = async + new transaction + after-commit; the Modulith
 * JPA registry persists and retries the publication, so the billing eventually reflects the
 * order even across a crash or a transient conflict.
 */
@Component
@RequiredArgsConstructor
class OrderBillingSyncEventListener {

    private final SyncOrderBillingUseCase syncOrderBilling;

    @ApplicationModuleListener
    void on(OrderBillingSyncEvent event) {
        syncOrderBilling.sync(event.orderId());
    }
}