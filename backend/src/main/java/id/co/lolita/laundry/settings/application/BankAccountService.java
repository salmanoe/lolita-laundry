package id.co.lolita.laundry.settings.application;

import id.co.lolita.laundry.settings.domain.BankAccount;
import id.co.lolita.laundry.settings.domain.port.in.BankAccountQuery;
import id.co.lolita.laundry.settings.domain.port.in.ManageBankAccountUseCase;
import id.co.lolita.laundry.settings.domain.port.out.BankAccountRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Manages the company's bank accounts and exposes them to other modules.
 *
 * <p>Two invariants live here rather than in the aggregate because both span rows: exactly one
 * account is the default, and the default can never be deactivated (it is the fallback every
 * unassigned client bills to).
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
class BankAccountService implements ManageBankAccountUseCase, BankAccountQuery {

    private final BankAccountRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<BankAccount> list() {
        return repository.findAll();
    }

    @Override
    public BankAccount create(CreateBankAccountCommand command) {
        // The very first account has to be the default, otherwise nothing would resolve.
        boolean first = repository.findDefault().isEmpty();
        return repository.save(new BankAccount(null, command.label(), command.beneficiary(), command.bankName(),
                command.accountNumber(), command.accountHolder(), first, true, command.sortOrder()));
    }

    @Override
    public BankAccount update(UpdateBankAccountCommand command) {
        var account = require(command.id());
        account.update(command.label(), command.beneficiary(), command.bankName(), command.accountNumber(),
                command.accountHolder(), command.sortOrder());
        return repository.save(account);
    }

    @Override
    public BankAccount setDefault(Long id) {
        var account = require(id);
        if (account.isDefaultAccount()) {
            return account;
        }
        if (!account.isActive()) {
            throw new IllegalArgumentException("Rekening yang nonaktif tidak dapat dijadikan default");
        }
        // Clear first: the partial unique index on is_default is checked per statement, and the
        // repository flushes on save, so the old flag is gone before the new one lands.
        repository.findDefault().ifPresent(previous -> {
            previous.clearDefault();
            repository.save(previous);
        });
        account.markDefault();
        return repository.save(account);
    }

    @Override
    public BankAccount setActive(Long id, boolean active) {
        var account = require(id);
        if (!active && account.isDefaultAccount()) {
            throw new IllegalArgumentException(
                    "Rekening default tidak dapat dinonaktifkan. Tetapkan rekening default lain terlebih dahulu.");
        }
        if (active) {
            account.activate();
        } else {
            account.deactivate();
        }
        return repository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BankAccountView> findById(Long id) {
        return id == null ? Optional.empty() : repository.findById(id).map(BankAccountService::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccountView resolve(Long bankAccountId) {
        if (bankAccountId != null) {
            var assigned = repository.findById(bankAccountId);
            if (assigned.isPresent() && assigned.get().isActive()) {
                return toView(assigned.get());
            }
            // Deactivating an assigned account is not blocked (that check would need settings to
            // read the client module and would close a dependency cycle), so degrade to the
            // default rather than failing the render.
            log.warn("Bank account {} is missing or inactive — falling back to the default account", bankAccountId);
        }
        return toView(repository.findDefault().orElseGet(BankAccount::defaults));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountView> activeAccounts() {
        return repository.findActive().stream().map(BankAccountService::toView).toList();
    }

    private BankAccount require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bank account not found: " + id));
    }

    private static BankAccountView toView(BankAccount a) {
        return new BankAccountView(a.getId(), a.getLabel(), a.getBeneficiary(), a.getBankName(),
                a.getAccountNumber(), a.getAccountHolder(), a.isActive());
    }
}
