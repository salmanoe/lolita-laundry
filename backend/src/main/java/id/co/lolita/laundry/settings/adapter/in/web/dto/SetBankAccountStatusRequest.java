package id.co.lolita.laundry.settings.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record SetBankAccountStatusRequest(@NotNull Boolean active) {
}
