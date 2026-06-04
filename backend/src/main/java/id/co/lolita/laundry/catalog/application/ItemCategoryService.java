package id.co.lolita.laundry.catalog.application;

import id.co.lolita.laundry.catalog.domain.ItemCategory;
import id.co.lolita.laundry.catalog.domain.port.in.CreateLookupCommand;
import id.co.lolita.laundry.catalog.domain.port.in.ItemCategoryUseCase;
import id.co.lolita.laundry.catalog.domain.port.in.UpdateLookupCommand;
import id.co.lolita.laundry.catalog.domain.port.out.ItemCategoryRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class ItemCategoryService implements ItemCategoryUseCase {

    private final ItemCategoryRepository repository;

    @Override
    public List<ItemCategory> list() {
        return repository.findAll();
    }

    @Override
    public List<ItemCategory> listActive() {
        return repository.findAllActive();
    }

    @Override
    @Transactional
    public ItemCategory create(CreateLookupCommand command) {
        if (repository.existsByCode(command.code())) {
            throw new IllegalArgumentException("Category code '%s' already exists".formatted(command.code()));
        }
        return repository.save(new ItemCategory(null, command.code(), command.displayName(), command.sortOrder(), true));
    }

    @Override
    @Transactional
    public ItemCategory update(UpdateLookupCommand command) {
        var category = repository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Item category not found: " + command.id()));
        category.update(command.displayName(), command.sortOrder());
        if (command.active()) {
            category.activate();
        } else {
            category.deactivate();
        }
        return repository.save(category);
    }
}