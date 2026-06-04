/**
 * Storage module outbound port. Exposed as a Spring Modulith named interface so other
 * modules (e.g. {@code order}) may store and serve objects (delivery photos, PDFs)
 * through {@link id.co.lolita.laundry.storage.domain.port.out.StoragePort}.
 */
@NamedInterface("api")
package id.co.lolita.laundry.storage.domain.port.out;

import org.springframework.modulith.NamedInterface;