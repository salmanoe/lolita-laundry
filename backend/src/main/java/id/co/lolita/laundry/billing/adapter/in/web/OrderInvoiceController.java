package id.co.lolita.laundry.billing.adapter.in.web;

import id.co.lolita.laundry.billing.adapter.in.web.dto.OrderInvoiceResponse;
import id.co.lolita.laundry.billing.domain.port.in.CreateOrderInvoiceUseCase;
import id.co.lolita.laundry.billing.domain.port.in.GetBillingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Per-order invoice lookup. Mapped under {@code /api/orders/{id}/invoice} (the documented
 * path) but owned by the billing module — the path string creates no module dependency on
 * order. Available from RECEIVED onward: a live preview while the order is open, frozen once
 * delivered.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
class OrderInvoiceController {

    private final GetBillingUseCase billingQuery;
    private final CreateOrderInvoiceUseCase invoiceWriter;

    @GetMapping("/api/orders/{orderId}/invoice")
    OrderInvoiceResponse get(@PathVariable Long orderId) {
        // Creates/refreshes the invoice on demand (live preview while open, frozen once
        // delivered), then returns metadata + a short-lived pre-signed URL. 404 for an unknown
        // or canceled order.
        var invoice = invoiceWriter.prepareInvoiceForOrder(orderId);
        var pdfUrl = billingQuery.getInvoicePdfUrlForOrder(orderId);
        return OrderInvoiceResponse.from(invoice, pdfUrl);
    }
}