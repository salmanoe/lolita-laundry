package id.co.lolita.laundry.settings.adapter.in.web;

import id.co.lolita.laundry.settings.adapter.in.web.dto.BankAccountOptionResponse;
import id.co.lolita.laundry.settings.adapter.in.web.dto.BankAccountRequest;
import id.co.lolita.laundry.settings.adapter.in.web.dto.BankAccountResponse;
import id.co.lolita.laundry.settings.adapter.in.web.dto.SetBankAccountStatusRequest;
import id.co.lolita.laundry.settings.domain.port.in.ManageBankAccountUseCase;
import id.co.lolita.laundry.settings.domain.port.in.ManageBankAccountUseCase.CreateBankAccountCommand;
import id.co.lolita.laundry.settings.domain.port.in.ManageBankAccountUseCase.UpdateBankAccountCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SUPER_ADMIN-only bank-account administration, on the Master Data screen alongside the company
 * profile. The accounts are printed in the transfer block of every monthly-billing PDF; billing
 * reads them through the settings::api gateway, not this endpoint, so locking the REST read to
 * SUPER_ADMIN does not affect rendering.
 */
@RestController
@RequestMapping("/api/bank-accounts")
@RequiredArgsConstructor
class BankAccountController {

    private final ManageBankAccountUseCase bankAccounts;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    List<BankAccountResponse> list() {
        return bankAccounts.list().stream().map(BankAccountResponse::from).toList();
    }

    /**
     * Labels only, no transfer details — the client screens use this to render and pick a client's
     * account, so FINANCE_STAFF (who read the client list) need it too. Inactive accounts are
     * included so an existing assignment still renders a name rather than a blank.
     */
    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('FINANCE_STAFF', 'SUPER_ADMIN')")
    List<BankAccountOptionResponse> options() {
        return bankAccounts.list().stream().map(BankAccountOptionResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    BankAccountResponse create(@Valid @RequestBody BankAccountRequest request) {
        return BankAccountResponse.from(bankAccounts.create(new CreateBankAccountCommand(
                request.label(), request.beneficiary(), request.bankName(), request.accountNumber(),
                request.accountHolder(), request.sortOrder())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    BankAccountResponse update(@PathVariable Long id, @Valid @RequestBody BankAccountRequest request) {
        return BankAccountResponse.from(bankAccounts.update(new UpdateBankAccountCommand(
                id, request.label(), request.beneficiary(), request.bankName(), request.accountNumber(),
                request.accountHolder(), request.sortOrder())));
    }

    @PostMapping("/{id}/default")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    BankAccountResponse setDefault(@PathVariable Long id) {
        return BankAccountResponse.from(bankAccounts.setDefault(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    BankAccountResponse setActive(@PathVariable Long id, @Valid @RequestBody SetBankAccountStatusRequest request) {
        return BankAccountResponse.from(bankAccounts.setActive(id, request.active()));
    }
}
