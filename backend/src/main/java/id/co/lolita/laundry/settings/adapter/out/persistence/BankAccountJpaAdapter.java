package id.co.lolita.laundry.settings.adapter.out.persistence;

import id.co.lolita.laundry.settings.domain.BankAccount;
import id.co.lolita.laundry.settings.domain.port.out.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class BankAccountJpaAdapter implements BankAccountRepository {

    private final BankAccountJpaRepository jpaRepository;

    @Override
    public List<BankAccount> findAll() {
        return jpaRepository.findAllByOrderBySortOrderAscLabelAsc().stream()
                .map(BankAccountJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<BankAccount> findActive() {
        return jpaRepository.findAllByActiveTrueOrderBySortOrderAscLabelAsc().stream()
                .map(BankAccountJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<BankAccount> findById(Long id) {
        return jpaRepository.findById(id).map(BankAccountJpaEntity::toDomain);
    }

    @Override
    public Optional<BankAccount> findDefault() {
        return jpaRepository.findByDefaultAccountTrue().map(BankAccountJpaEntity::toDomain);
    }

    /**
     * Writes through immediately rather than at transaction commit. Promoting a new default is two
     * updates — clear the old flag, set the new one — and the partial unique index on
     * {@code is_default} is checked per statement, so the clear has to reach the database before
     * the set. Flushing here keeps that ordering explicit instead of relying on Hibernate's
     * flush order.
     */
    @Override
    public BankAccount save(BankAccount account) {
        var entity = account.getId() == null
                ? BankAccountJpaEntity.fromDomain(account)
                : jpaRepository.findById(account.getId()).orElseGet(() -> BankAccountJpaEntity.fromDomain(account));
        entity.applyScalars(account);
        return jpaRepository.saveAndFlush(entity).toDomain();
    }
}
