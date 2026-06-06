/**
 * Catalog module inbound ports. Exposed as a Spring Modulith named interface so other
 * modules (e.g. {@code order}) may depend on the read-only
 * {@link id.co.lolita.laundry.catalog.domain.port.in.CatalogQuery} without reaching into
 * the catalog module's internals.
 */
@NamedInterface("api")
package id.co.lolita.laundry.catalog.domain.port.in;

import org.springframework.modulith.NamedInterface;