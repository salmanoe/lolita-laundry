/**
 * Order module inbound ports. Exposed as a Spring Modulith named interface so other modules
 * may depend on the read-only {@link id.co.lolita.laundry.order.domain.port.in.DeliveredOrderQuery}
 * (used by {@code billing} to produce invoices) without reaching into the order module's
 * internals. The remaining use-case ports here are consumed only by the order module's own
 * web adapters; exposing the whole package mirrors the {@code catalog} and {@code client}
 * convention and is harmless — no other module references them.
 */
@NamedInterface("api")
package id.co.lolita.laundry.order.domain.port.in;

import org.springframework.modulith.NamedInterface;