package id.co.lolita.laundry.client.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ClientTypeJpaRepository extends JpaRepository<ClientTypeJpaEntity, Long> {
    List<ClientTypeJpaEntity> findAllByOrderBySortOrderAscIdAsc();

    List<ClientTypeJpaEntity> findByActiveTrueOrderBySortOrderAscIdAsc();

    boolean existsByCode(String code);
}