package id.co.lolita.laundry.billing.adapter.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request to generate the monthly billing(s) for a client and period.
 */
public record GenerateBillingRequest(
        @NotNull Long clientId,
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotNull @Min(1) @Max(12) Integer month
) {
}