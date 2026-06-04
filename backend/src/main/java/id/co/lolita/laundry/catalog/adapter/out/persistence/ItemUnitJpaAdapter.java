package id.co.lolita.laundry.catalog.adapter.out.persistence;

import id.co.lolita.laundry.catalog.domain.ItemUnit;
import id.co.lolita.laundry.catalog.domain.port.out.ItemUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ItemUnitJpaAdapter implements ItemUnitRepository {

    private final ItemUnitJpaRepository jpaRepository;

    @Override
    public List<ItemUnit> findAll() {
        return jpaRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(ItemUnitJpaEntity::toDomain).toList();
    }

    @Override
    public List<ItemUnit> findAllActive() {
        return jpaRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream().map(ItemUnitJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<ItemUnit> findById(Long id) {
        return jpaRepository.findById(id).map(ItemUnitJpaEntity::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public ItemUnit save(ItemUnit unit) {
        return jpaRepository.save(ItemUnitJpaEntity.fromDomain(unit)).toDomain();
    }
}
