package id.co.lolita.laundry.catalog.adapter.in.web;

import id.co.lolita.laundry.catalog.adapter.in.web.dto.CreateItemRequest;
import id.co.lolita.laundry.catalog.adapter.in.web.dto.ItemResponse;
import id.co.lolita.laundry.catalog.adapter.in.web.dto.UpdateItemRequest;
import id.co.lolita.laundry.catalog.domain.port.in.GetItemsUseCase;
import id.co.lolita.laundry.catalog.domain.port.in.ManageItemUseCase;
import id.co.lolita.laundry.catalog.domain.port.in.ManageItemUseCase.CreateItemCommand;
import id.co.lolita.laundry.catalog.domain.port.in.ManageItemUseCase.UpdateItemCommand;
import id.co.lolita.laundry.shared.Page;
import id.co.lolita.laundry.shared.PageQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
class ItemController {

    private final GetItemsUseCase getItems;
    private final ManageItemUseCase manageItem;

    // Item management screen is SUPER_ADMIN-only (the paginated list backs ItemsPage). FINANCE_STAFF
    // still resolve item names/units through /options for orders and price-setting.
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    Page<ItemResponse> listItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search
    ) {
        return getItems.getItems(PageQuery.of(page, size, sort, direction), search).map(ItemResponse::from);
    }

    /**
     * Unpaged list of active items for selection dropdowns (e.g. setting a client price).
     */
    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('DAILY_STAFF', 'FINANCE_STAFF', 'SUPER_ADMIN')")
    List<ItemResponse> listItemOptions() {
        return getItems.getActiveItems().stream().map(ItemResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    ItemResponse createItem(@Valid @RequestBody CreateItemRequest request) {
        var command = new CreateItemCommand(request.name(), request.unitId());
        return ItemResponse.from(manageItem.createItem(command));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    ItemResponse updateItem(@PathVariable Long id, @Valid @RequestBody UpdateItemRequest request) {
        var command = new UpdateItemCommand(id, request.name(), request.unitId(), request.active());
        return ItemResponse.from(manageItem.updateItem(command));
    }
}
