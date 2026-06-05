package id.co.lolita.laundry.order.domain.port.out;

/**
 * Order module's view of the {@code user} module, used to validate a driver assignment.
 * The adapter delegates to the user module's exposed query API.
 */
public interface DriverGateway {

    /**
     * True when the id belongs to an active user with the DRIVER role.
     */
    boolean isActiveDriver(Long userId);
}