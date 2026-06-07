package id.co.lolita.laundry.billing.adapter.in.web.dto;

/**
 * Wraps a short-lived pre-signed URL to a billing PDF.
 */
public record BillingPdfUrlResponse(String url) {
}