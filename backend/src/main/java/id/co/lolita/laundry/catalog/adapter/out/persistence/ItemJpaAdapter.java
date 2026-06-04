package id.co.lolita.laundry.catalog.adapter.out.persistence;

import id.co.lolita.laundry.catalog.domain.ItemMaster;
import id.co.lolita.laundry.catalog.domain.port.out.ItemRepository;
import id.co.lolita.laundry.shared.Page;
import id.co.lolita.laundry.shared.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ItemJpaAdapter implements ItemRepository {

    private final ItemJpaRepository jpaRepository;

    @Override
    public Page<ItemMaster> findAll(PageQuery query) {
        var springPage = jpaRepository.findAll(PageMapper.toPageable(query));
        return new Page<>(
                springPage.getContent().stream().map(ItemJpaEntity::toDomain).toList(),
                springPage.getNumber(), springPage.getSize(),
                springPage.getTotalElements(), springPage.getTotalPages()
        );
    }

    @Override
    public List<ItemMaster> findAllActive() {
        return jpaRepository.findByActiveTrue().stream().map(ItemJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<ItemMaster> findById(Long id) {
        return jpaRepository.findById(id).map(ItemJpaEntity::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public ItemMaster save(ItemMaster item) {
        return jpaRepository.save(ItemJpaEntity.fromDomain(item)).toDomain();
    }
}
