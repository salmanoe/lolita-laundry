package id.co.lolita.laundry.settings.adapter.in.web.dto;

import id.co.lolita.laundry.settings.domain.BankAccount;

public record BankAccountResponse(Long id, String label, String beneficiary, String bankName, String accountNumber,
                                  String accountHolder, boolean defaultAccount, boolean active, int sortOrder) {

    public static BankAccountResponse from(BankAccount a) {
        return new BankAccountResponse(a.getId(), a.getLabel(), a.getBeneficiary(), a.getBankName(),
                a.getAccountNumber(), a.getAccountHolder(), a.isDefaultAccount(), a.isActive(), a.getSortOrder());
    }
}
