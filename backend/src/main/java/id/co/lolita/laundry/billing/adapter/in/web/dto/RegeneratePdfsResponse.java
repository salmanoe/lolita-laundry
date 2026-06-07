package id.co.lolita.laundry.billing.adapter.in.web.dto;

/**
 * Result of the bulk "Perbarui Semua PDF" action — how many of each document were re-rendered.
 */
public record RegeneratePdfsResponse(int billings, int invoices) {
}