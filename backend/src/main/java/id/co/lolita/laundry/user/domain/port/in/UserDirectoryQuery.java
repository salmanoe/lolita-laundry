package id.co.lolita.laundry.user.domain.port.in;

import java.util.List;
import java.util.Optional;

/**
 * Cross-module lookup to attribute actions to a Lolita staff user. Returns the user's
 * database id for a given Auth0 {@code sub} (empty if the sub is not provisioned).
 */
public interface UserDirectoryQuery {

    /**
     * Minimal driver projection for assignment dropdowns — id + display name, no prices, no identity keys.
     */
    record DriverSummary(Long id, String fullName) {
    }

    Optional<Long> idForAuth0Sub(String auth0Sub);

    /**
     * Active users with the DRIVER role, for the staff "assign to driver" picker.
     */
    List<DriverSummary> activeDrivers();

    /**
     * True when the given user id exists, is active, and has the DRIVER role.
     */
    boolean isActiveDriver(Long userId);
}
