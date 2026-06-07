package id.co.lolita.laundry.billing.adapter.out.persistence;

import id.co.lolita.laundry.billing.domain.MonthlyBilling;
import id.co.lolita.laundry.billing.domain.port.out.MonthlyBillingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class MonthlyBillingJpaAdapter implements MonthlyBillingRepository {

    private final MonthlyBillingJpaRepository jpaRepository;

    @Override
    public MonthlyBilling save(MonthlyBilling billing) {
        MonthlyBillingJpaEntity entity;
        if (billing.getId() == null) {
            entity = MonthlyBillingJpaEntity.newFromDomain(billing);
        } else {
            // Status, attached PDF, total and the line set can all change after creation
            // (the billing is auto-maintained as orders are received/edited/canceled).
            entity = jpaRepository.findById(billing.getId())
                    .orElseThrow(() -> new IllegalStateException("Billing vanished while saving: " + billing.getId()));
            entity.applyMutable(billing);
        }
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<MonthlyBilling> findByOrderLine(Long orderId) {
        return jpaRepository.findByOrderLine(orderId).map(MonthlyBillingJpaEntity::toDomain);
    }

    @Override
    public Optional<MonthlyBilling> findById(Long id) {
        return jpaRepository.findById(id).map(MonthlyBillingJpaEntity::toDomain);
    }

    @Override
    public List<MonthlyBilling> findAll(Long clientId, Integer year, Integer month) {
        return jpaRepository.search(clientId, year, month).stream()
                .map(MonthlyBillingJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<MonthlyBilling> findExisting(Long clientId, Long departmentId, int year, int month) {
        return jpaRepository.findExisting(clientId, departmentId, year, month)
                .map(MonthlyBillingJpaEntity::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        // Flush so the DELETE hits the DB before the replacement INSERT, which would otherwise
        // collide on UNIQUE(client_id, department_id, period_year, period_month).
        jpaRepository.deleteById(id);
        jpaRepository.flush();
    }
}