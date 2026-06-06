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

        var pdfBytes = pdf.renderOrderInvoice(toDocument(invoice, order, client.name(), client.clientCode()));
        var key = storage.store("invoices/" + invoiceNumber + ".pdf", pdfBytes);
        invoice.attachPdf(key);

        invoiceRepository.save(invoice);
        log.info("Generated order invoice {} for order {}", invoiceNumber, order.orderNumber());
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
                order.departmentName() == null ? "" : order.departmentName(),
                items,
                BillingFormats.money(invoice.getSubtotal()));
    }
}