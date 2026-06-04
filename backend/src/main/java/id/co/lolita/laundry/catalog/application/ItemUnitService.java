package id.co.lolita.laundry.catalog.application;

import id.co.lolita.laundry.catalog.domain.ItemUnit;
import id.co.lolita.laundry.catalog.domain.port.in.CreateLookupCommand;
import id.co.lolita.laundry.catalog.domain.port.in.ItemUnitUseCase;
import id.co.lolita.laundry.catalog.domain.port.in.UpdateLookupCommand;
import id.co.lolita.laundry.catalog.domain.port.out.ItemUnitRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class ItemUnitService implements ItemUnitUseCase {

    private final ItemUnitRepository repository;

    @Override
    public List<ItemUnit> list() {
        return repository.findAll();
    }

    @Override
    public List<ItemUnit> listActive() {
        return repository.findAllActive();
    }

    @Override
    @Transactional
    public ItemUnit create(CreateLookupCommand command) {
        if (repository.existsByCode(command.code())) {
            throw new IllegalArgumentException("Unit code '%s' already exists".formatted(command.code()));
        }
        return repository.save(new ItemUnit(null, command.code(), command.displayName(), command.sortOrder(), true));
    }

    @Override
    @Transactional
    public ItemUnit update(UpdateLookupCommand command) {
        var unit = repository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Item unit not found: " + command.id()));
        unit.update(command.displayName(), command.sortOrder());
        if (command.active()) {
            unit.activate();
        } else {
            unit.deactivate();
        }
        return repository.save(unit);
    }
}