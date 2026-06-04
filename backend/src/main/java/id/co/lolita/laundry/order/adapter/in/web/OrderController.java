package id.co.lolita.laundry.order.adapter.in.web;

import id.co.lolita.laundry.order.adapter.in.web.dto.*;
import id.co.lolita.laundry.order.domain.OrderQuery;
import id.co.lolita.laundry.order.domain.OrderStatus;
import id.co.lolita.laundry.order.domain.port.in.*;
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
 * Authenticated order operations for Lolita staff (OWNER / STAFF).
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
class OrderController {

    private final GetOrdersUseCase ordersQuery;
    private final CreateOrderUseCase createOrder;
    private final UpdateOrderUseCase updateOrder;
    private final UpdateOrderStatusUseCase updateStatus;
    private final DeliverOrderUseCase deliverOrder;
    private final CurrentUserResolver currentUser;

    @GetMapping
    Page<OrderSummaryResponse> list(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        var query = new OrderQuery(clientId, status, from, to, PageQuery.of(page, size, sort, direction));
        return ordersQuery.getOrders(query).map(OrderSummaryResponse::from);
    }

    @GetMapping("/{id}")
    OrderResponse get(@PathVariable Long id) {
        return OrderResponse.from(ordersQuery.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse create(@Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        var command = new CreateOrderCommand(
                request.clientId(), request.departmentId(), request.treatment(), request.dueDate(),
                request.submittedByName(), request.notes(), currentUser.currentUserId(authentication),
                request.items().stream().map(OrderLineRequest::toInput).toList());
        return OrderResponse.from(createOrder.createOrder(command));
    }

    @PutMapping("/{id}")
    OrderResponse update(@PathVariable Long id, @Valid @RequestBody UpdateOrderRequest request) {
        var items = request.items() == null ? null
                : request.items().stream().map(OrderLineRequest::toInput).toList();
        return OrderResponse.from(
                updateOrder.updateOrder(new UpdateOrderCommand(id, request.dueDate(), request.notes(), items)));
    }

    @PatchMapping("/{id}/status")
    OrderResponse advanceStatus(@PathVariable Long id, @Valid @RequestBody AdvanceStatusRequest request,
                                Authentication authentication) {
        var command = new AdvanceStatusCommand(
                id, request.status(), currentUser.currentUserId(authentication), request.notes());
        return OrderResponse.from(updateStatus.advanceStatus(command));
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

    @PostMapping(path = "/{id}/delivery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
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
