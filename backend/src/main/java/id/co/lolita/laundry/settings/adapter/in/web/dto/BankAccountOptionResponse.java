package id.co.lolita.laundry.settings.adapter.in.web.dto;

import id.co.lolita.laundry.settings.domain.BankAccount;

/**
 * The minimal shape used to label and pick an account on the client screens — no transfer details,
 * so a broader audience than the Master Data editor can read it.
 */
public record BankAccountOptionResponse(Long id, String label, boolean defaultAccount, boolean active) {

    public static BankAccountOptionResponse from(BankAccount a) {
        return new BankAccountOptionResponse(a.getId(), a.getLabel(), a.isDefaultAccount(), a.isActive());
    }
}
