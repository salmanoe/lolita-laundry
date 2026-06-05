package id.co.lolita.laundry.order.domain.port.in;

import id.co.lolita.laundry.order.domain.Order;

/**
 * Staff (OWNER / STAFF) assign an order to a driver, or unassign it. The driver then sees
 * the order on their delivery screen and confirms delivery there.
 */
public interface AssignDriverUseCase {

    /**
     * {@code driverId} null means unassign.
     */
    record AssignDriverCommand(Long orderId, Long driverId) {
    }

    Order assignDriver(AssignDriverCommand command);
}