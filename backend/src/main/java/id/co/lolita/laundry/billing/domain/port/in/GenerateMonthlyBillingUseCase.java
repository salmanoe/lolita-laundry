package id.co.lolita.laundry.billing.domain.port.in;

import id.co.lolita.laundry.billing.domain.MonthlyBilling;

import java.util.List;

/**
 * Generates the monthly billing(s) for a client and period by aggregating that month's
 * delivered orders. COMBINED clients yield exactly one billing; PER_DEPARTMENT clients (PBS)
 * yield one per department that has delivered orders.
 *
 * <p>Regeneration policy: an existing DRAFT for the period is replaced (orders may have
 * changed); an ISSUED or PAID billing is locked and blocks regeneration.
 */
public interface GenerateMonthlyBillingUseCase {

    record GenerateCommand(Long clientId, int year, int month) {
    }

    List<MonthlyBilling> generate(GenerateCommand command);

    /**
     * Returns the billing, lazily rendering and storing its PDF if it has none yet. A monthly
     * billing normally gets its PDF as it is auto-synced, but a storage outage during sync can
     * leave {@code pdf_url} null (the event is retried, but the row may be viewed before then);
     * this heals it on first view, mirroring the per-order invoice self-heal. Idempotent — a
     * no-op once the PDF exists. Throws if the billing does not exist.
     */
    MonthlyBilling ensurePdfForBilling(Long id);

    /**
     * Re-renders and re-stores the PDF for every existing billing (layout-only — totals, period
     * and status are unchanged), so a template change reaches documents already generated,
     * including ISSUED/PAID ones. Returns how many were refreshed.
     */
    int regenerateAllPdfs();
}