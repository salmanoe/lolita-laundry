package id.co.lolita.laundry.settings.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload for a bank account. Sizes match the {@code bank_accounts} DDL. The default
 * flag and the active flag are not editable here — they have their own endpoints because both
 * carry cross-row rules.
 */
public record BankAccountRequest(
        @NotBlank @Size(max = 50) String label,
        @NotBlank @Size(max = 100) String beneficiary,
        @NotBlank @Size(max = 50) String bankName,
        @NotBlank @Size(max = 50) String accountNumber,
        @NotBlank @Size(max = 100) String accountHolder,
        @PositiveOrZero int sortOrder
) {
}
