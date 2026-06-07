package id.co.lolita.laundry.order.domain.event;

import java.time.Instant;

/**
 * Domain event published when an order transitions to {@code DELIVERED}. Consumed
 * cross-module by {@code billing}, which generates the per-order invoice (Phase 3).
 *
 * <p>Pure domain record — no Spring. Published by the application service via Spring's
 * {@code ApplicationEventPublisher}; persisted by the Modulith JPA event registry so the
 * invoice is still produced if the listener fails after delivery commits. Lives in an
 * exposed named interface ({@code order::events}) because the consuming module references it.
 *
 * @param orderId     the delivered order's id
 * @param orderNumber its business number (e.g. {@code PBS-20260601-001}) — drives the invoice number
 * @param clientId    the owning client
 * @param deliveredAt when delivery was confirmed
 */
public record OrderDeliveredEvent(Long orderId, String orderNumber, Long clientId, Instant deliveredAt) {
}