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
}