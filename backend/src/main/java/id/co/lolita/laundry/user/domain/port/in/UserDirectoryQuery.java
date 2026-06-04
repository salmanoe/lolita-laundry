package id.co.lolita.laundry.user.domain.port.in;

import java.util.Optional;

/**
 * Cross-module lookup to attribute actions to a Lolita staff user. Returns the user's
 * database id for a given Auth0 {@code sub} (empty if the sub is not provisioned).
 */
public interface UserDirectoryQuery {

    Optional<Long> idForAuth0Sub(String auth0Sub);
}
