package id.co.lolita.laundry.billing.domain.port.out;

import java.util.List;

/**
 * Renders billing documents to PDF bytes. Implemented by the JasperReports adapter
 * ({@code adapter/out/pdf}). All monetary and date fields are pre-formatted strings so the
 * report templates only print text — no locale or arithmetic logic lives in the .jrxml.
 */
public interface InvoicePdfPort {

    /** A line on the itemized Order Invoice. */
    record InvoiceItemRow(String name, String unit, String quantity, String unitPrice, String subtotal) {
    }

    /** Everything the Order Invoice template needs. */
    record OrderInvoiceDocument(String invoiceNumber, String invoiceDate, String clientName, String clientCode,
                                String orderNumber, String orderDate, String orderTypeLabel,
                                String departmentName, List<InvoiceItemRow> items, String total) {
    }

    /** A line on the Monthly Billing (one delivered order). */
    record BillingOrderRow(String orderNumber, String orderDate, String subtotal) {
    }

    /** Everything the Monthly Billing template needs. */
    record MonthlyBillingDocument(String billingNumber, String periodLabel, String clientName, String clientCode,
                                  String departmentName, String invoiceDate, List<BillingOrderRow> orders,
                                  String total) {
    }

    byte[] renderOrderInvoice(OrderInvoiceDocument document);

    byte[] renderMonthlyBilling(MonthlyBillingDocument document);
}