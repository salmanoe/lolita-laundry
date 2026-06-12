package id.co.lolita.laundry.client.adapter.in.web;

import id.co.lolita.laundry.client.adapter.in.web.dto.CreateLookupRequest;
import id.co.lolita.laundry.client.adapter.in.web.dto.LookupResponse;
import id.co.lolita.laundry.client.adapter.in.web.dto.UpdateLookupRequest;
import id.co.lolita.laundry.client.domain.port.in.ClientTypeUseCase;
import id.co.lolita.laundry.client.domain.port.in.CreateLookupCommand;
import id.co.lolita.laundry.client.domain.port.in.UpdateLookupCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client-types")
@RequiredArgsConstructor
class ClientTypeController {

    private final ClientTypeUseCase clientTypes;

    // Reference-data lists stay readable by FINANCE_STAFF (they resolve type labels for clients);
    // SUPER_ADMIN reads them on the Master Data screen. Mutations are SUPER_ADMIN-only.
    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_STAFF', 'SUPER_ADMIN')")
    List<LookupResponse> list() {
        return clientTypes.list().stream().map(LookupResponse::from).toList();
    }

    /**
     * Active client types only, for selection dropdowns.
     */
    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('FINANCE_STAFF', 'SUPER_ADMIN')")
    List<LookupResponse> options() {
        return clientTypes.listActive().stream().map(LookupResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    LookupResponse create(@Valid @RequestBody CreateLookupRequest request) {
        return LookupResponse.from(clientTypes.create(
                new CreateLookupCommand(request.code(), request.displayName(), request.sortOrder())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    LookupResponse update(@PathVariable Long id, @Valid @RequestBody UpdateLookupRequest request) {
        return LookupResponse.from(clientTypes.update(
                new UpdateLookupCommand(id, request.displayName(), request.sortOrder(), request.active())));
    }
}