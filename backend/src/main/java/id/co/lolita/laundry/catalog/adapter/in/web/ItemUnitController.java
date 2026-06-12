package id.co.lolita.laundry.catalog.adapter.in.web;

import id.co.lolita.laundry.catalog.adapter.in.web.dto.CreateLookupRequest;
import id.co.lolita.laundry.catalog.adapter.in.web.dto.LookupResponse;
import id.co.lolita.laundry.catalog.adapter.in.web.dto.UpdateLookupRequest;
import id.co.lolita.laundry.catalog.domain.port.in.CreateLookupCommand;
import id.co.lolita.laundry.catalog.domain.port.in.ItemUnitUseCase;
import id.co.lolita.laundry.catalog.domain.port.in.UpdateLookupCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item-units")
@RequiredArgsConstructor
class ItemUnitController {

    private final ItemUnitUseCase itemUnits;

    // Reference-data lists stay readable by DAILY_STAFF/FINANCE_STAFF (they resolve unit labels for
    // the order create/edit screens); SUPER_ADMIN reads them on the Master Data screen. Mutations are
    // SUPER_ADMIN-only.
    @GetMapping
    @PreAuthorize("hasAnyRole('DAILY_STAFF', 'FINANCE_STAFF', 'SUPER_ADMIN')")
    List<LookupResponse> list() {
        return itemUnits.list().stream().map(LookupResponse::from).toList();
    }

    /**
     * Active units only, for selection dropdowns.
     */
    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('FINANCE_STAFF', 'SUPER_ADMIN')")
    List<LookupResponse> options() {
        return itemUnits.listActive().stream().map(LookupResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    LookupResponse create(@Valid @RequestBody CreateLookupRequest request) {
        return LookupResponse.from(itemUnits.create(
                new CreateLookupCommand(request.code(), request.displayName(), request.sortOrder())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    LookupResponse update(@PathVariable Long id, @Valid @RequestBody UpdateLookupRequest request) {
        return LookupResponse.from(itemUnits.update(
                new UpdateLookupCommand(id, request.displayName(), request.sortOrder(), request.active())));
    }
}