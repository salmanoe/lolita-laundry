package id.co.lolita.laundry.catalog.application;

import id.co.lolita.laundry.catalog.domain.ItemMaster;
import id.co.lolita.laundry.catalog.domain.ItemUnit;
import id.co.lolita.laundry.catalog.domain.port.in.CatalogQuery;
import id.co.lolita.laundry.catalog.domain.port.in.GetItemsUseCase;
import id.co.lolita.laundry.catalog.domain.port.in.ManageItemUseCase;
import id.co.lolita.laundry.catalog.domain.port.out.ItemRepository;
import id.co.lolita.laundry.catalog.domain.port.out.ItemUnitRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import id.co.lolita.laundry.shared.Page;
import id.co.lolita.laundry.shared.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class ItemService implements GetItemsUseCase, ManageItemUseCase, CatalogQuery {

    private final ItemRepository itemRepository;
    private final ItemUnitRepository unitRepository;

    @Override
    public Page<ItemMaster> getItems(PageQuery query) {
        return itemRepository.findAll(query);
    }

    @Override
    public List<ItemMaster> getActiveItems() {
        return itemRepository.findAllActive();
    }

    // ── CatalogQuery (cross-module read API) ──

    @Override
    public List<CatalogItemSnapshot> activeItems() {
        var unitNames = unitRepository.findAll().stream()
                .collect(Collectors.toMap(ItemUnit::getId, ItemUnit::getDisplayName));
        return itemRepository.findAllActive().stream()
                .map(item -> toSnapshot(item, unitNames.get(item.getUnitId())))
                .toList();
    }

    @Override
    public Optional<CatalogItemSnapshot> findActiveById(Long itemId) {
        return itemRepository.findById(itemId)
                .filter(ItemMaster::isActive)
                .map(item -> toSnapshot(item,
                        unitRepository.findById(item.getUnitId()).map(ItemUnit::getDisplayName).orElse(null)));
    }

    private static CatalogItemSnapshot toSnapshot(ItemMaster item, String unitName) {
        return new CatalogItemSnapshot(item.getId(), item.getName(), item.getUnitId(), unitName);
    }

    @Override
    @Transactional
    public ItemMaster createItem(CreateItemCommand command) {
        if (itemRepository.existsByName(command.name())) {
            throw new IllegalArgumentException("Item with name '%s' already exists".formatted(command.name()));
        }
        requireUnitExists(command.unitId());
        var item = new ItemMaster(null, command.name(), command.unitId(), true);
        return itemRepository.save(item);
    }

    @Override
    @Transactional
    public ItemMaster updateItem(UpdateItemCommand command) {
        var item = itemRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Item not found: " + command.id()));

        requireUnitExists(command.unitId());
        item.update(command.name(), command.unitId());
        if (command.active()) {
            item.activate();
        } else {
            item.deactivate();
        }
        return itemRepository.save(item);
    }

    /**
     * 404 if the referenced unit does not exist (clearer than a raw FK 409).
     */
    private void requireUnitExists(Long unitId) {
        if (unitRepository.findById(unitId).isEmpty()) {
            throw new NotFoundException("Item unit not found: " + unitId);
        }
    }
}