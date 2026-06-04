package id.co.lolita.laundry.catalog.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ItemUnitJpaRepository extends JpaRepository<ItemUnitJpaEntity, Long> {
    List<ItemUnitJpaEntity> findAllByOrderBySortOrderAscIdAsc();

    List<ItemUnitJpaEntity> findByActiveTrueOrderBySortOrderAscIdAsc();

    boolean existsByCode(String code);
}