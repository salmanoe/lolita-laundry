package id.co.lolita.laundry.client.application;

import id.co.lolita.laundry.client.domain.ClientType;
import id.co.lolita.laundry.client.domain.port.in.ClientTypeUseCase;
import id.co.lolita.laundry.client.domain.port.in.CreateLookupCommand;
import id.co.lolita.laundry.client.domain.port.in.UpdateLookupCommand;
import id.co.lolita.laundry.client.domain.port.out.ClientTypeRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class ClientTypeService implements ClientTypeUseCase {

    private final ClientTypeRepository repository;

    @Override
    public List<ClientType> list() {
        return repository.findAll();
    }

    @Override
    public List<ClientType> listActive() {
        return repository.findAllActive();
    }

    @Override
    @Transactional
    public ClientType create(CreateLookupCommand command) {
        if (repository.existsByCode(command.code())) {
            throw new IllegalArgumentException("Client type code '%s' already exists".formatted(command.code()));
        }
        return repository.save(new ClientType(null, command.code(), command.displayName(), command.sortOrder(), true));
    }

    @Override
    @Transactional
    public ClientType update(UpdateLookupCommand command) {
        var type = repository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Client type not found: " + command.id()));
        type.update(command.displayName(), command.sortOrder());
        if (command.active()) {
            type.activate();
        } else {
            type.deactivate();
        }
        return repository.save(type);
    }
}