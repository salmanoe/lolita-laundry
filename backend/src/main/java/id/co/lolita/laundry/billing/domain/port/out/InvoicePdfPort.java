package id.co.lolita.laundry.billing.domain.port.out;

import java.util.List;

/**
 * Renders billing documents to PDF bytes. Implemented by the JasperReports adapter
 * ({@code adapter/out/pdf}). All monetary and date fields are pre-formatted strings so the
 * report templates only print text — no locale or arithmetic logic lives in the .jrxml.
 */
public interface InvoicePdfPort {

    /**
     * A line on the itemized Order Invoice.
     */
    record InvoiceItemRow(String name, String unit, String quantity, String unitPrice, String subtotal) {
    }

    /**
     * Everything the Order Invoice template needs.
     */
    record OrderInvoiceDocument(String invoiceNumber, String invoiceDate, String clientName, String clientCode,
                                String orderNumber, String orderDate, String orderTypeLabel,
                                String departmentName, List<InvoiceItemRow> items, String total) {
    }

    /**
     * Everything the Monthly Billing invoice template needs. The client-facing document is a
     * single "Laundry Periode" line with the grand total (the per-order breakdown lives in the
     * app UI, not on the invoice), plus the Terbilang amount-in-words.
     */
    record MonthlyBillingDocument(String number, String clientName, String departmentName, String invoiceDate,
                                  String paymentTerms, String periodDescription, String total,
                                  String amountInWords) {
    }

    byte[] renderOrderInvoice(OrderInvoiceDocument document);

    byte[] renderMonthlyBilling(MonthlyBillingDocument document);
}