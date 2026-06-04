package id.co.lolita.laundry.catalog.adapter.out.persistence;

import id.co.lolita.laundry.catalog.domain.ItemCategory;
import id.co.lolita.laundry.catalog.domain.port.out.ItemCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ItemCategoryJpaAdapter implements ItemCategoryRepository {

    private final ItemCategoryJpaRepository jpaRepository;

    @Override
    public List<ItemCategory> findAll() {
        return jpaRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(ItemCategoryJpaEntity::toDomain).toList();
    }

    @Override
    public List<ItemCategory> findAllActive() {
        return jpaRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream().map(ItemCategoryJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<ItemCategory> findById(Long id) {
        return jpaRepository.findById(id).map(ItemCategoryJpaEntity::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public ItemCategory save(ItemCategory category) {
        return jpaRepository.save(ItemCategoryJpaEntity.fromDomain(category)).toDomain();
    }
}
