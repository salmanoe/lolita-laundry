/**
 * Settings module inbound ports. Exposed as a Spring Modulith named interface so other modules
 * (e.g. {@code billing}) may read the company profile via
 * {@link id.co.lolita.laundry.settings.domain.port.in.CompanyProfileQuery} without reaching into
 * the settings module's internals.
 */
@NamedInterface("api")
package id.co.lolita.laundry.settings.domain.port.in;

import org.springframework.modulith.NamedInterface;