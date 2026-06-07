package id.co.lolita.laundry.client.adapter.in.web;

import id.co.lolita.laundry.client.adapter.in.web.dto.*;
import id.co.lolita.laundry.client.domain.port.in.*;
import id.co.lolita.laundry.client.domain.port.in.ManageClientUseCase.CreateClientCommand;
import id.co.lolita.laundry.client.domain.port.in.ManageClientUseCase.UpdateClientCommand;
import id.co.lolita.laundry.client.domain.port.in.ManageDepartmentUseCase.CreateDepartmentCommand;
import id.co.lolita.laundry.client.domain.port.in.ManageDepartmentUseCase.UpdateDepartmentCommand;
import id.co.lolita.laundry.client.domain.port.in.ManagePriceListUseCase.SetPriceCommand;
import id.co.lolita.laundry.shared.Page;
import id.co.lolita.laundry.shared.PageQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
class ClientController {

    private final GetClientUseCase clientQuery;
    private final ManageClientUseCase manageClient;
    private final ManageDepartmentUseCase manageDepartment;
    private final ManagePriceListUseCase managePriceList;

    // ── Clients ──

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    Page<ClientResponse> listClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return clientQuery.getClients(PageQuery.of(page, size, sort, direction)).map(ClientResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    ClientResponse getClient(@PathVariable Long id) {
        return ClientResponse.from(clientQuery.getClientById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    ClientResponse createClient(@Valid @RequestBody CreateClientRequest request) {
        var command = new CreateClientCommand(
                request.name(), request.clientCode(), request.clientTypeId(), request.billingMode(),
                request.contactPerson(), request.phone(), request.address()
        );
        return ClientResponse.from(manageClient.createClient(command));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    ClientResponse updateClient(@PathVariable Long id, @Valid @RequestBody UpdateClientRequest request) {
        var command = new UpdateClientCommand(
                id, request.name(), request.clientTypeId(), request.billingMode(),
                request.contactPerson(), request.phone(), request.address()
        );
        return ClientResponse.from(manageClient.updateClient(command));
    }

    @PostMapping("/{id}/rotate-token")
    @PreAuthorize("hasRole('OWNER')")
    ClientResponse rotateToken(@PathVariable Long id) {
        return ClientResponse.from(manageClient.rotateToken(id));
    }

    // ── Departments ──

    @GetMapping("/{clientId}/departments")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    List<DepartmentResponse> listDepartments(@PathVariable Long clientId) {
        return manageDepartment.getDepartmentsByClient(clientId).stream()
                .map(DepartmentResponse::from).toList();
    }

    @PostMapping("/{clientId}/departments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    DepartmentResponse createDepartment(@PathVariable Long clientId,
                                        @Valid @RequestBody DepartmentRequest request) {
        return DepartmentResponse.from(
                manageDepartment.createDepartment(new CreateDepartmentCommand(clientId, request.name()))
        );
    }

    @PutMapping("/{clientId}/departments/{deptId}")
    @PreAuthorize("hasRole('OWNER')")
    DepartmentResponse updateDepartment(@PathVariable Long clientId, @PathVariable Long deptId,
                                        @Valid @RequestBody DepartmentRequest request) {
        return DepartmentResponse.from(
                manageDepartment.updateDepartment(new UpdateDepartmentCommand(deptId, request.name(), request.active()))
        );
    }

    // ── Price list ──

    @GetMapping("/{clientId}/prices")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    List<PriceListResponse> getPrices(@PathVariable Long clientId) {
        return managePriceList.getCurrentPrices(clientId).stream()
                .map(PriceListResponse::from).toList();
    }

    @PostMapping("/{clientId}/prices")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    PriceListResponse setPrice(@PathVariable Long clientId, @Valid @RequestBody SetPriceRequest request) {
        var command = new SetPriceCommand(clientId, request.itemId(), request.pricePerUnit(),
                request.effectiveDate(), request.departmentId());
        var saved = managePriceList.setPrice(command);
        return new PriceListResponse(saved.itemId(), saved.pricePerUnit(), saved.effectiveDate(),
                request.departmentId());
    }
}
