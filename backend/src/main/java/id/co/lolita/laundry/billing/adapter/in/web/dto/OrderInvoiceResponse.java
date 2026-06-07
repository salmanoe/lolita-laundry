package id.co.lolita.laundry.billing.adapter.in.web.dto;

import id.co.lolita.laundry.billing.domain.OrderInvoice;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Per-order invoice metadata plus a short-lived pre-signed URL to view the PDF.
 */
public record OrderInvoiceResponse(String invoiceNumber, Long orderId, LocalDate invoiceDate,
                                   BigDecimal subtotal, String pdfUrl) {

    public static OrderInvoiceResponse from(OrderInvoice invoice, String pdfUrl) {
        return new OrderInvoiceResponse(invoice.getInvoiceNumber(), invoice.getOrderId(),
                invoice.getInvoiceDate(), invoice.getSubtotal(), pdfUrl);
    }
}