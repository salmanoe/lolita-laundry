package id.co.lolita.laundry.order.adapter.in.web.dto;

/**
 * Optional reason for cancelling (voiding) an order.
 */
public record CancelOrderRequest(String notes) {
}