package id.co.lolita.laundry.billing.adapter.in.web.dto;

/**
 * Result of a manual billing re-sync: how many of the client's orders were re-run through the
 * order → billing sync.
 */
public record ResyncBillingResponse(int resyncedOrders) {
}
