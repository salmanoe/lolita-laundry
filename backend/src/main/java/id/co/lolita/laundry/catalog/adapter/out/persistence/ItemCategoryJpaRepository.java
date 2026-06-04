package id.co.lolita.laundry.catalog.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ItemCategoryJpaRepository extends JpaRepository<ItemCategoryJpaEntity, Long> {
    List<ItemCategoryJpaEntity> findAllByOrderBySortOrderAscIdAsc();

    List<ItemCategoryJpaEntity> findByActiveTrueOrderBySortOrderAscIdAsc();

    boolean existsByCode(String code);
}
