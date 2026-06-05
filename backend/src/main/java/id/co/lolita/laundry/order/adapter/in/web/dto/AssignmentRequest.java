package id.co.lolita.laundry.order.adapter.in.web.dto;

/**
 * Assign an order to a driver. A null {@code driverId} unassigns.
 */
public record AssignmentRequest(Long driverId) {
}