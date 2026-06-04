/**
 * User module inbound ports. Exposed as a Spring Modulith named interface so other modules
 * (e.g. {@code order}) can resolve the acting user's id from their Auth0 {@code sub} via
 * {@link id.co.lolita.laundry.user.domain.port.in.UserDirectoryQuery}.
 */
@NamedInterface("api")
package id.co.lolita.laundry.user.domain.port.in;

import org.springframework.modulith.NamedInterface;