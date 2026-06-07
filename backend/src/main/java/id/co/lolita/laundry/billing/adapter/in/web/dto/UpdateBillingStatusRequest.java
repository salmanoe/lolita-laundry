package id.co.lolita.laundry.billing.adapter.in.web.dto;

import id.co.lolita.laundry.billing.domain.BillingStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request to advance a monthly billing's status one step (DRAFT → ISSUED → PAID).
 */
public record UpdateBillingStatusRequest(@NotNull BillingStatus status) {
}