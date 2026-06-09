package id.co.lolita.laundry.client.application;

import id.co.lolita.laundry.client.domain.BillingMode;
import id.co.lolita.laundry.client.domain.Client;
import id.co.lolita.laundry.client.domain.ClientItemDepartment;
import id.co.lolita.laundry.client.domain.ClientPriceList;
import id.co.lolita.laundry.client.domain.Department;
import id.co.lolita.laundry.client.domain.port.in.*;
import id.co.lolita.laundry.client.domain.port.out.ClientItemDepartmentRepository;
import id.co.lolita.laundry.client.domain.port.out.ClientPriceListRepository;
import id.co.lolita.laundry.client.domain.port.out.ClientRepository;
import id.co.lolita.laundry.client.domain.port.out.ClientTypeRepository;
import id.co.lolita.laundry.client.domain.port.out.DepartmentRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import id.co.lolita.laundry.shared.Page;
import id.co.lolita.laundry.shared.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class ClientService implements GetClientUseCase, ManageClientUseCase, ManageDepartmentUseCase,
        ManagePriceListUseCase, ClientDirectoryQuery, ClientPricingQuery {

    private final ClientRepository clientRepository;
    private final DepartmentRepository departmentRepository;
    private final ClientPriceListRepository priceListRepository;
    private final ClientItemDepartmentRepository itemDepartmentRepository;
    private final ClientTypeRepository clientTypeRepository;

    // ── GetClientUseCase ──

    @Override
    public Page<Client> getClients(PageQuery query) {
        return clientRepository.findAll(query);
    }

    @Override
    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found: " + id));
    }

    @Override
    public Client getClientByToken(UUID token) {
        return clientRepository.findByOrderToken(token)
                .filter(Client::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive order token"));
    }

    // ── ManageClientUseCase ──

    @Override
    @Transactional
    public Client createClient(CreateClientCommand command) {
        if (clientRepository.existsByClientCode(command.clientCode())) {
            throw new IllegalArgumentException("Client code '%s' is already in use".formatted(command.clientCode()));
        }
        requireClientTypeExists(command.clientTypeId());
        var client = new Client(
                null, command.name(), command.clientCode(),
                command.clientTypeId(), command.billingMode(),
                command.contactPerson(), command.phone(), command.address(),
                UUID.randomUUID(), true, Instant.now()
        );
        return clientRepository.save(client);
    }

    @Override
    @Transactional
    public Client updateClient(UpdateClientCommand command) {
        var client = getClientById(command.id());
        requireClientTypeExists(command.clientTypeId());
        client.update(command.name(), command.clientTypeId(), command.billingMode(),
                command.contactPerson(), command.phone(), command.address());
        return clientRepository.save(client);
    }

    private void requireClientTypeExists(Long clientTypeId) {
        if (clientTypeRepository.findById(clientTypeId).isEmpty()) {
            throw new NotFoundException("Client type not found: " + clientTypeId);
        }
    }

    @Override
    @Transactional
    public Client rotateToken(Long clientId) {
        var client = getClientById(clientId);
        client.rotateToken();
        return clientRepository.save(client);
    }

    // ── ManageDepartmentUseCase ──

    @Override
    public List<Department> getDepartmentsByClient(Long clientId) {
        return departmentRepository.findByClientId(clientId);
    }

    @Override
    @Transactional
    public Department createDepartment(CreateDepartmentCommand command) {
        getClientById(command.clientId());  // 404 if the client doesn't exist (vs. a raw FK 500)
        var department = new Department(null, command.clientId(), command.name(), true);
        return departmentRepository.save(department);
    }

    @Override
    @Transactional
    public Department updateDepartment(UpdateDepartmentCommand command) {
        var department = departmentRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Department not found: " + command.id()));
        department.rename(command.name());
        if (command.active()) {
            department.activate();
        } else {
            department.deactivate();
        }
        return departmentRepository.save(department);
    }

    // ── ManagePriceListUseCase ──

    @Override
    public List<CurrentPrice> getCurrentPrices(Long clientId) {
        Map<Long, Long> deptByItem = itemDepartmentRepository.findByClient(clientId).stream()
                .collect(Collectors.toMap(ClientItemDepartment::itemId, ClientItemDepartment::departmentId));
        return priceListRepository.findCurrentPrices(clientId).stream()
                .map(p -> new CurrentPrice(p.itemId(), p.pricePerUnit(), p.effectiveDate(),
                        deptByItem.get(p.itemId())))
                .toList();
    }

    @Override
    @Transactional
    public ClientPriceList setPrice(SetPriceCommand command) {
        var client = getClientById(command.clientId());  // 404 if the client doesn't exist
        applyItemDepartment(client, command.itemId(), command.departmentId());
        var date = command.effectiveDate() != null ? command.effectiveDate() : LocalDate.now();
        // A new effective date appends a history row; re-saving an existing date corrects that
        // row in place (otherwise UNIQUE(client_id, item_id, effective_date) would reject it).
        var entry = priceListRepository.findExact(command.clientId(), command.itemId(), date)
                .map(existing -> new ClientPriceList(existing.id(), existing.clientId(), existing.itemId(),
                        command.pricePerUnit(), existing.effectiveDate(), existing.createdAt()))
                .orElseGet(() -> new ClientPriceList(
                        null, command.clientId(), command.itemId(), command.pricePerUnit(), date, Instant.now()));
        return priceListRepository.save(entry);
    }

    /**
     * Upserts (or clears) the item→department assignment for a priced item. For a
     * PER_DEPARTMENT client the department is required and must belong to the client; for a
     * COMBINED client a department must not be supplied (and any stale mapping is cleared).
     */
    private void applyItemDepartment(Client client, Long itemId, Long departmentId) {
        if (client.getBillingMode() == BillingMode.PER_DEPARTMENT) {
            if (departmentId == null) {
                throw new IllegalArgumentException("A department is required when pricing an item for this client");
            }
            if (!departmentBelongsToClient(departmentId, client.getId())) {
                throw new IllegalArgumentException("Department %d does not belong to client %d"
                        .formatted(departmentId, client.getId()));
            }
            itemDepartmentRepository.upsert(client.getId(), itemId, departmentId);
        } else {
            if (departmentId != null) {
                throw new IllegalArgumentException("Department cannot be set for a combined-billing client");
            }
            itemDepartmentRepository.delete(client.getId(), itemId);
        }
    }

    // ── ClientDirectoryQuery (cross-module read API) ──

    @Override
    public Optional<ClientView> findByToken(UUID token) {
        return clientRepository.findByOrderToken(token).map(ClientService::toView);
    }

    @Override
    public Optional<ClientView> findById(Long clientId) {
        return clientRepository.findById(clientId).map(ClientService::toView);
    }

    @Override
    public List<DepartmentView> activeDepartments(Long clientId) {
        return departmentRepository.findByClientId(clientId).stream()
                .filter(Department::isActive)
                .map(d -> new DepartmentView(d.getId(), d.getName()))
                .toList();
    }

    @Override
    public List<DepartmentView> allDepartments(Long clientId) {
        // No active filter — historical labels must resolve even for a deactivated department.
        return departmentRepository.findByClientId(clientId).stream()
                .map(d -> new DepartmentView(d.getId(), d.getName()))
                .toList();
    }

    @Override
    public boolean departmentBelongsToClient(Long departmentId, Long clientId) {
        return departmentRepository.findById(departmentId)
                .filter(d -> d.getClientId().equals(clientId))
                .isPresent();
    }

    private static ClientView toView(Client c) {
        return new ClientView(c.getId(), c.getName(), c.getClientCode(),
                c.isActive(), c.getBillingMode() == BillingMode.PER_DEPARTMENT);
    }

    // ── ClientPricingQuery (cross-module read API) ──

    @Override
    public Optional<BigDecimal> effectivePrice(Long clientId, Long itemId, LocalDate asOf) {
        return priceListRepository.findEffectivePrice(clientId, itemId, asOf)
                .map(ClientPriceList::pricePerUnit);
    }

    @Override
    public List<PricePoint> currentPrices(Long clientId) {
        return priceListRepository.findCurrentPrices(clientId).stream()
                .map(p -> new PricePoint(p.itemId(), p.pricePerUnit()))
                .toList();
    }

    @Override
    public List<ItemDepartment> itemDepartments(Long clientId) {
        return itemDepartmentRepository.findByClient(clientId).stream()
                .map(m -> new ItemDepartment(m.itemId(), m.departmentId()))
                .toList();
    }

    @Override
    public Optional<Long> departmentForItem(Long clientId, Long itemId) {
        return itemDepartmentRepository.find(clientId, itemId).map(ClientItemDepartment::departmentId);
    }
}
