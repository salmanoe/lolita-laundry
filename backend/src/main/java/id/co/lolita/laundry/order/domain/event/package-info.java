/**
 * Order module domain events published cross-module. Exposed as a Spring Modulith named
 * interface ({@code events}) so consuming modules (e.g. {@code billing}) may listen for
 * {@link id.co.lolita.laundry.order.domain.event.OrderDeliveredEvent} without reaching into
 * the order module's internals.
 */
@NamedInterface("events")
package id.co.lolita.laundry.order.domain.event;

import org.springframework.modulith.NamedInterface;