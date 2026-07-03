package id.co.lolita.laundry.order.adapter.in.web;

import id.co.lolita.laundry.order.adapter.in.web.dto.*;
import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderQuery;
import id.co.lolita.laundry.order.domain.OrderStatus;
import id.co.lolita.laundry.order.domain.port.in.*;
import id.co.lolita.laundry.order.domain.port.in.CancelOrderUseCase.CancelOrderCommand;
import id.co.lolita.laundry.order.domain.port.in.CreateOrderUseCase.CreateOrderCommand;
import id.co.lolita.laundry.order.domain.port.in.DeliverOrderUseCase.DeliverOrderCommand;
import id.co.lolita.laundry.order.domain.port.in.UpdateOrderStatusUseCase.AdvanceStatusCommand;
import id.co.lolita.laundry.order.domain.port.in.UpdateOrderUseCase.UpdateOrderCommand;
import id.co.lolita.laundry.shared.NotFoundException;
import id.co.lolita.laundry.shared.Page;
import id.co.lolita.laundry.shared.PageQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Authenticated order operations, open to all three roles (DAILY_STAFF / FINANCE_STAFF /
 * SUPER_ADMIN): reads, creation, edit, status advance, and cancel. The only method-level
 * restriction left is the not-surfaced staff-fallback delivery confirm (FINANCE_STAFF/SUPER_ADMIN);
 * the normal delivery path is the operator app (`/api/deliveries`).
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DAILY_STAFF', 'FINANCE_STAFF', 'SUPER_ADMIN')")
class OrderController {

    private final GetOrdersUseCase ordersQuery;
    private final GetOrderFormUseCase orderForm;
    private final CreateOrderUseCase createOrder;
    private final UpdateOrderUseCase updateOrder;
    private final UpdateOrderStatusUseCase updateStatus;
    private final CancelOrderUseCase cancelOrder;
    private final DeliverOrderUseCase deliverOrder;
    private final CurrentUserResolver currentUser;

    /**
     * Order-form data for the in-house "Buat Order" screen: the selected client's priced items
     * (grouped by department for PER_DEPARTMENT clients) and whether Treatment pricing applies.
     * Replaces the retired public tokenized form — staff pick the hotel from a dropdown.
     */
    @GetMapping("/form")
    OrderFormResponse form(@RequestParam Long clientId) {
        return OrderFormResponse.from(orderForm.getOrderForm(clientId));
    }

    @GetMapping
    Page<OrderSummaryResponse> list(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication
    ) {
        var query = new OrderQuery(clientId, status, from, to, PageQuery.of(page, size, sort, direction));
        java.util.function.Function<Order, OrderSummaryResponse> mapper = priceFree(authentication)
                ? OrderSummaryResponse::priceFree : OrderSummaryResponse::from;
        return ordersQuery.getOrders(query).map(mapper);
    }

    @GetMapping("/{id}")
    OrderResponse get(@PathVariable Long id, Authentication authentication) {
        return respond(ordersQuery.getById(id), authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse create(@Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        var command = new CreateOrderCommand(
                request.clientId(), request.treatment(), request.dueDate(),
                request.submittedByName(), request.notes(), currentUser.currentUserId(authentication),
                request.items().stream().map(OrderLineRequest::toInput).toList());
        return respond(createOrder.createOrder(command), authentication);
    }

    @PutMapping("/{id}")
    OrderResponse update(@PathVariable Long id, @Valid @RequestBody UpdateOrderRequest request,
                         Authentication authentication) {
        var items = request.items() == null ? null
                : request.items().stream().map(OrderLineRequest::toInput).toList();
        // The order date and the Treatment flag are SUPER_ADMIN-only corrections; a lower role's
        // values are ignored server-side (defense in depth — the React form only shows those fields
        // to SUPER_ADMIN).
        boolean superAdmin = canCorrectOrder(authentication);
        var orderDate = superAdmin ? request.orderDate() : null;
        var treatment = superAdmin ? request.treatment() : null;
        return respond(
                updateOrder.updateOrder(
                        new UpdateOrderCommand(id, orderDate, treatment, request.dueDate(), request.notes(), items)),
                authentication);
    }

    // Only SUPER_ADMIN may correct the order date or the Treatment flag. In the dev profile (security
    // disabled) there are no authorities, so we fail open — matching how the admin screens behave for
    // an unresolved role.
    private boolean canCorrectOrder(Authentication authentication) {
        return authentication == null || authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    @PatchMapping("/{id}/status")
    OrderResponse advanceStatus(@PathVariable Long id, @Valid @RequestBody AdvanceStatusRequest request,
                                Authentication authentication) {
        var command = new AdvanceStatusCommand(
                id, request.status(), currentUser.currentUserId(authentication), request.notes());
        return respond(updateStatus.advanceStatus(command), authentication);
    }

    @PostMapping("/{id}/cancel")
    OrderResponse cancel(@PathVariable Long id, @RequestBody(required = false) CancelOrderRequest request,
                         Authentication authentication) {
        var notes = request == null ? null : request.notes();
        return respond(cancelOrder.cancel(
                new CancelOrderCommand(id, currentUser.currentUserId(authentication), notes)), authentication);
    }

    // DAILY_STAFF are a price-free operator role: strip total/multiplier/line prices from the order
    // response server-side (defense in depth — the React UI also hides them). In the dev profile
    // (security disabled) there are no authorities, so prices are shown — fine for local dev.
    private OrderResponse respond(Order order, Authentication authentication) {
        return priceFree(authentication) ? OrderResponse.priceFree(order) : OrderResponse.from(order);
    }

    private boolean priceFree(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DAILY_STAFF".equals(a.getAuthority()));
    }

    @GetMapping("/{id}/history")
    List<StatusHistoryResponse> history(@PathVariable Long id) {
        return ordersQuery.getHistory(id).stream().map(StatusHistoryResponse::from).toList();
    }

    @GetMapping("/{id}/delivery")
    DeliveryConfirmationResponse getDelivery(@PathVariable Long id) {
        return ordersQuery.getDelivery(id)
                .map(DeliveryConfirmationResponse::from)
                .orElseThrow(() -> new NotFoundException("No delivery confirmation for order " + id));
    }

    @GetMapping("/{id}/delivery/photo-url")
    PhotoUrlResponse getDeliveryPhotoUrl(@PathVariable Long id) {
        return new PhotoUrlResponse(ordersQuery.getDeliveryPhotoUrl(id)
                .orElseThrow(() -> new NotFoundException("No delivery photo for order " + id)));
    }

    /**
     * Staff delivery confirmation. Backend-only fallback — the normal path is the driver app
     * (`POST /api/deliveries/{id}/confirm`); this is intentionally not surfaced in the staff UI,
     * kept only so an owner/staff can close an order when no driver is available. Same contract
     * as the driver endpoint (mandatory photo, order must be at DONE).
     */
    @PostMapping(path = "/{id}/delivery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('FINANCE_STAFF', 'SUPER_ADMIN')")
    DeliveryConfirmationResponse deliver(
            @PathVariable Long id,
            @RequestParam String recipientName,
            @RequestParam String delivererName,
            @RequestParam(required = false) String notes,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            Authentication authentication
    ) {
        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException("A delivery photo is required");
        }
        byte[] bytes;
        try {
            bytes = photo.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded photo");
        }
        var command = new DeliverOrderCommand(
                id, recipientName, delivererName, notes, bytes,
                photo.getContentType(), photo.getOriginalFilename(), currentUser.currentUserId(authentication));
        return DeliveryConfirmationResponse.from(deliverOrder.deliver(command));
    }
}
