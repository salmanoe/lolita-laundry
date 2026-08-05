package id.co.lolita.laundry.settings.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface BankAccountJpaRepository extends JpaRepository<BankAccountJpaEntity, Long> {

    List<BankAccountJpaEntity> findAllByOrderBySortOrderAscLabelAsc();

    List<BankAccountJpaEntity> findAllByActiveTrueOrderBySortOrderAscLabelAsc();

    Optional<BankAccountJpaEntity> findByDefaultAccountTrue();
}
