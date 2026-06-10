package id.co.lolita.laundry.billing.application;

import id.co.lolita.laundry.billing.domain.OrderInvoice;
import id.co.lolita.laundry.billing.domain.port.in.CreateOrderInvoiceUseCase;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway;
import id.co.lolita.laundry.billing.domain.port.out.BillingStoragePort;
import id.co.lolita.laundry.billing.domain.port.out.CompanyProfileGateway;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway.DeliveredOrder;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.CompanyHeader;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.InvoiceItemRow;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.OrderInvoiceDocument;
import id.co.lolita.laundry.billing.domain.port.out.OrderInvoiceRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Generates the per-order invoice. The invoice is viewable from the moment the order is
 * RECEIVED: {@link #prepareInvoiceForOrder} creates-or-refreshes a live <em>preview</em> while
 * the order is still open, and the {@code OrderDeliveredEvent} listener calls
 * {@link #createForDeliveredOrder} to render the authoritative, frozen invoice at delivery.
 * Both paths are idempotent — there is a single invoice row per order (keyed on order id), so
 * event redelivery and repeated views never duplicate it.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
class OrderInvoiceService implements CreateOrderInvoiceUseCase {

    private static final String INVOICE_NUMBER_PREFIX = "INV-";

    private final OrderInvoiceRepository invoiceRepository;
    private final DeliveredOrderGateway deliveredOrders;
    private final BillingClientGateway clients;
    private final CompanyProfileGateway companyProfile;
    private final InvoicePdfPort pdf;
    private final BillingStoragePort storage;

    @Override
    public void createForDeliveredOrder(Long orderId) {
        // Delivery is the authoritative freeze point: render from the final order state,
        // overwriting any preview produced while the order was still open. Idempotent — event
        // redelivery just re-renders the same delivered state into the same invoice row.
        var invoice = renderInvoiceForOrder(orderId);
        log.info("Froze order invoice {} at delivery (order {})",
                invoice.getInvoiceNumber(), orderId);
    }

    @Override
    public OrderInvoice prepareInvoiceForOrder(Long orderId) {
        var existing = invoiceRepository.findByOrderId(orderId);
        // A delivered order is final: once its invoice has a PDF, never touch it again so a later
        // company-profile change can't rewrite a settled document.
        if (existing.isPresent() && hasPdf(existing.get())
                && deliveredOrders.findBillableOrder(orderId).map(DeliveredOrder::delivered).orElse(false)) {
            return existing.get();
        }
        // Otherwise (no invoice yet, or the order is still open) create-or-refresh the preview
        // from the current order/company state.
        return renderInvoiceForOrder(orderId);
    }

    /**
     * Creates the invoice (first call) or refreshes the existing one, then renders, stores and
     * saves it. Membership is "any billable (non-canceled) order", so the invoice is available
     * from RECEIVED onward; a canceled/unknown order has no invoice (404).
     */
    private OrderInvoice renderInvoiceForOrder(Long orderId) {
        DeliveredOrder order = deliveredOrders.findBillableOrder(orderId)
                .orElseThrow(() -> new NotFoundException("No billable order to invoice: " + orderId));
        var client = clients.findById(order.clientId())
                .orElseThrow(() -> new IllegalStateException(
                        "Client not found for invoicing order " + orderId));
        var company = companyProfile.current();

        var invoice = invoiceRepository.findByOrderId(orderId)
                .map(existing -> {
                    existing.refresh(LocalDate.now(), order.total(),
                            company.companyName(), company.address(), company.phone());
                    return existing;
                })
                .orElseGet(() -> OrderInvoice.create(
                        INVOICE_NUMBER_PREFIX + order.orderNumber(), orderId, order.clientId(),
                        LocalDate.now(), order.total(),
                        company.companyName(), company.address(), company.phone()));

        renderStoreAndAttachPdf(invoice, order, client.name(), client.clientCode());
        return invoiceRepository.save(invoice);
    }

    private static boolean hasPdf(OrderInvoice invoice) {
        return invoice.getPdfUrl() != null && !invoice.getPdfUrl().isBlank();
    }

    @Override
    public int regenerateAllPdfs() {
        int count = 0;
        for (OrderInvoice invoice : invoiceRepository.findAll()) {
            var order = deliveredOrders.findBillableOrder(invoice.getOrderId()).orElse(null);
            if (order == null) {
                log.warn("Skipping PDF refresh for invoice {} — billable order {} not found",
                        invoice.getInvoiceNumber(), invoice.getOrderId());
                continue;
            }
            var client = clients.findById(order.clientId()).orElse(null);
            if (client == null) {
                log.warn("Skipping PDF refresh for invoice {} — client {} not found",
                        invoice.getInvoiceNumber(), order.clientId());
                continue;
            }
            renderStoreAndAttachPdf(invoice, order, client.name(), client.clientCode());
            invoiceRepository.save(invoice);
            count++;
        }
        log.info("Refreshed {} order-invoice PDFs", count);
        return count;
    }

    /**
     * Renders the invoice PDF, stores it, and attaches the storage key to the invoice.
     */
    private void renderStoreAndAttachPdf(OrderInvoice invoice, DeliveredOrder order,
                                         String clientName, String clientCode) {
        var pdfBytes = pdf.renderOrderInvoice(toDocument(invoice, order, clientName, clientCode));
        var key = storage.store("invoices/" + invoice.getInvoiceNumber() + ".pdf", pdfBytes);
        invoice.attachPdf(key);
    }

    private OrderInvoiceDocument toDocument(OrderInvoice invoice, DeliveredOrder order,
                                            String clientName, String clientCode) {
        List<InvoiceItemRow> items = order.lines().stream()
                .map(l -> new InvoiceItemRow(
                        l.itemName(),
                        l.unit() == null ? "" : l.unit(),
                        BillingFormats.quantity(l.quantity()),
                        BillingFormats.money(l.unitPrice()),
                        BillingFormats.money(l.subtotal())))
                .toList();

        boolean treatment = order.pricingMultiplier() != null
                && order.pricingMultiplier().compareTo(BigDecimal.ONE) > 0;

        // Letterhead is the invoice's own snapshot, frozen at creation — no bank block on the
        // order invoice.
        var company = new CompanyHeader(invoice.getCompanyName(), invoice.getCompanyAddress(),
                invoice.getCompanyPhone(), null, null, null, null);

        return new OrderInvoiceDocument(
                company,
                invoice.getInvoiceNumber(),
                BillingFormats.longDate(invoice.getInvoiceDate()),
                clientName,
                clientCode,
                order.orderNumber(),
                BillingFormats.longDate(order.orderDate()),
                treatment ? "Treatment" : "Reguler",
                // An order may span departments now — the per-order invoice stays itemized and
                // does not carry a single department label.
                "",
                items,
                BillingFormats.money(invoice.getSubtotal()));
    }
}