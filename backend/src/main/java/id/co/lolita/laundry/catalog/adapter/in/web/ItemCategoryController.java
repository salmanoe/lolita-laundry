package id.co.lolita.laundry.catalog.adapter.in.web;

import id.co.lolita.laundry.catalog.adapter.in.web.dto.CreateLookupRequest;
import id.co.lolita.laundry.catalog.adapter.in.web.dto.LookupResponse;
import id.co.lolita.laundry.catalog.adapter.in.web.dto.UpdateLookupRequest;
import id.co.lolita.laundry.catalog.domain.port.in.CreateLookupCommand;
import id.co.lolita.laundry.catalog.domain.port.in.ItemCategoryUseCase;
import id.co.lolita.laundry.catalog.domain.port.in.UpdateLookupCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item-categories")
@RequiredArgsConstructor
class ItemCategoryController {

    private final ItemCategoryUseCase itemCategories;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    List<LookupResponse> list() {
        return itemCategories.list().stream().map(LookupResponse::from).toList();
    }

    /**
     * Active categories only, for selection dropdowns.
     */
    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    List<LookupResponse> options() {
        return itemCategories.listActive().stream().map(LookupResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    LookupResponse create(@Valid @RequestBody CreateLookupRequest request) {
        return LookupResponse.from(itemCategories.create(
                new CreateLookupCommand(request.code(), request.displayName(), request.sortOrder())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    LookupResponse update(@PathVariable Long id, @Valid @RequestBody UpdateLookupRequest request) {
        return LookupResponse.from(itemCategories.update(
                new UpdateLookupCommand(id, request.displayName(), request.sortOrder(), request.active())));
    }
}