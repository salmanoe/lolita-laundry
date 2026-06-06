/**
 * Client module inbound ports. Exposed as a Spring Modulith named interface so other
 * modules (e.g. {@code order}) may depend on the read-only query ports
 * ({@link id.co.lolita.laundry.client.domain.port.in.ClientDirectoryQuery},
 * {@link id.co.lolita.laundry.client.domain.port.in.ClientPricingQuery}) without reaching
 * into the client module's internals.
 */
@NamedInterface("api")
package id.co.lolita.laundry.client.domain.port.in;

import org.springframework.modulith.NamedInterface;