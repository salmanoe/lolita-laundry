package id.co.lolita.laundry.settings.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyProfileRequest(
        @NotBlank @Size(max = 100) String companyName,
        @NotBlank @Size(max = 200) String address,
        @NotBlank @Size(max = 30) String phone,
        @NotBlank @Size(max = 100) String bankBeneficiary,
        @NotBlank @Size(max = 50) String bankName,
        @NotBlank @Size(max = 50) String bankAccount,
        @NotBlank @Size(max = 100) String bankHolder
) {
}