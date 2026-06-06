package id.co.lolita.laundry.billing.domain.port.in;

import id.co.lolita.laundry.billing.domain.BillingStatus;
import id.co.lolita.laundry.billing.domain.MonthlyBilling;

/**
 * Advances a monthly billing's status one step ({@code DRAFT → ISSUED → PAID}).
 */
public interface UpdateBillingStatusUseCase {

    record UpdateStatusCommand(Long billingId, BillingStatus target) {
    }

    MonthlyBilling updateStatus(UpdateStatusCommand command);
}