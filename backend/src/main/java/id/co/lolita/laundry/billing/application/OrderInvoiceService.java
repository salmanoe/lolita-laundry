package id.co.lolita.laundry.billing.application;

import id.co.lolita.laundry.billing.domain.OrderInvoice;
import id.co.lolita.laundry.billing.domain.port.in.CreateOrderInvoiceUseCase;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway;
import id.co.lolita.laundry.billing.domain.port.out.BillingStoragePort;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway.DeliveredOrder;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort;
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
 * Generates the per-order invoice when an order is delivered. Invoked by the event adapter
 * in response to {@code OrderDeliveredEvent}. Idempotent so event redelivery (the Modulith
 * registry retries incomplete publications) never produces a duplicate invoice.
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
    private final InvoicePdfPort pdf;
    private final BillingStoragePort storage;

    @Override
    public void createForDeliveredOrder(Long orderId) {
        if (invoiceRepository.existsByOrderId(orderId)) {
            return;   // already invoiced — idempotent
        }

        DeliveredOrder order = deliveredOrders.findDeliveredOrder(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Delivered order not found for invoicing: " + orderId));
        var client = clients.findById(order.clientId())
                .orElseThrow(() -> new IllegalStateException(
                        "Client not found for invoicing order " + orderId));

        var invoiceNumber = INVOICE_NUMBER_PREFIX + order.orderNumber();
        var invoice = OrderInvoice.create(invoiceNumber, orderId, order.clientId(),
                LocalDate.now(), order.total());

        renderStoreAndAttachPdf(invoice, order, client.name(), client.clientCode());

        invoiceRepository.save(invoice);
        log.info("Generated order invoice {} for order {}", invoiceNumber, order.orderNumber());
    }

    @Override
    public OrderInvoice ensurePdfForOrder(Long orderId) {
        var invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("No invoice for order " + orderId));
        if (invoice.getPdfUrl() != null && !invoice.getPdfUrl().isBlank()) {
            return invoice;   // already rendered
        }

        var order = deliveredOrders.findDeliveredOrder(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Delivered order not found for invoicing: " + orderId));
        var client = clients.findById(order.clientId())
                .orElseThrow(() -> new IllegalStateException(
                        "Client not found for invoicing order " + orderId));

        renderStoreAndAttachPdf(invoice, order, client.name(), client.clientCode());
        var saved = invoiceRepository.save(invoice);
        log.info("Lazily rendered PDF for order invoice {} (order {})",
                invoice.getInvoiceNumber(), order.orderNumber());
        return saved;
    }

    @Override
    public int regenerateAllPdfs() {
        int count = 0;
        for (OrderInvoice invoice : invoiceRepository.findAll()) {
            var order = deliveredOrders.findDeliveredOrder(invoice.getOrderId()).orElse(null);
            if (order == null) {
                log.warn("Skipping PDF refresh for invoice {} — delivered order {} not found",
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

        return new OrderInvoiceDocument(
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