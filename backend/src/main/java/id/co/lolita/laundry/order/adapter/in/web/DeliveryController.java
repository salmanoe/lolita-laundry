package id.co.lolita.laundry.order.adapter.in.web;

import id.co.lolita.laundry.order.adapter.in.web.dto.DeliveryConfirmationResponse;
import id.co.lolita.laundry.order.adapter.in.web.dto.DriverDeliveryResponse;
import id.co.lolita.laundry.order.domain.port.in.DeliverOrderUseCase;
import id.co.lolita.laundry.order.domain.port.in.DeliverOrderUseCase.DeliverOrderCommand;
import id.co.lolita.laundry.order.domain.port.in.GetDriverDeliveriesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Delivery operations for the in-house DAILY_STAFF operators. They share one open pool: every
 * operator sees every order not yet delivered and can confirm any one — there is no per-operator
 * assignment. This shared-pool view is price-free, distinct from {@link OrderController}'s priced
 * order list. SUPER_ADMIN may also confirm (full access).
 */
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DAILY_STAFF', 'SUPER_ADMIN')")
class DeliveryController {

    private final GetDriverDeliveriesUseCase driverDeliveries;
    private final DeliverOrderUseCase deliverOrder;
    private final CurrentUserResolver currentUser;

    @GetMapping
    List<DriverDeliveryResponse> openDeliveries() {
        return driverDeliveries.getOpenDeliveries().stream()
                .map(DriverDeliveryResponse::from)
                .toList();
    }

    @PostMapping(path = "/{id}/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    DeliveryConfirmationResponse confirm(
            @PathVariable Long id,
            @RequestParam String recipientName,
            @RequestParam String delivererName,
            @RequestParam(required = false) String notes,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            Authentication authentication
    ) {
        // Open pool: any driver may confirm any order. The delivering driver is stamped on the
        // status history via byUserId; deliver() rejects anything not at DONE, so a second
        // driver confirming an already-delivered order fails cleanly.
        Long driverId = currentUser.currentUserId(authentication);
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
                photo.getContentType(), photo.getOriginalFilename(), driverId);
        return DeliveryConfirmationResponse.from(deliverOrder.deliver(command));
    }
}