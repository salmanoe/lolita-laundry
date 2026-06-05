package id.co.lolita.laundry.order.adapter.in.web;

import id.co.lolita.laundry.order.adapter.in.web.dto.DeliveryConfirmationResponse;
import id.co.lolita.laundry.order.adapter.in.web.dto.DriverDeliveryResponse;
import id.co.lolita.laundry.order.domain.port.in.DeliverOrderUseCase;
import id.co.lolita.laundry.order.domain.port.in.DeliverOrderUseCase.DeliverOrderCommand;
import id.co.lolita.laundry.order.domain.port.in.GetDriverDeliveriesUseCase;
import id.co.lolita.laundry.order.domain.port.in.GetOrdersUseCase;
import id.co.lolita.laundry.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Driver-facing delivery operations. A driver sees only the orders assigned to them and can
 * confirm delivery on those — never any pricing. Distinct from {@link OrderController}
 * (OWNER / STAFF) to keep the least privilege: the DRIVER role unlocks exactly these endpoints
 * and nothing else.
 */
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DRIVER')")
class DeliveryController {

    private final GetDriverDeliveriesUseCase driverDeliveries;
    private final GetOrdersUseCase ordersQuery;
    private final DeliverOrderUseCase deliverOrder;
    private final CurrentUserResolver currentUser;

    @GetMapping
    List<DriverDeliveryResponse> myDeliveries(Authentication authentication) {
        Long driverId = currentUser.currentUserId(authentication);
        return driverDeliveries.getAssignedDeliveries(driverId).stream()
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
        Long driverId = currentUser.currentUserId(authentication);
        var order = ordersQuery.getById(id);
        // Ownership: a driver may only confirm orders assigned to them. Treat a foreign order
        // as not found rather than revealing it exists.
        if (!Objects.equals(order.getAssignedDriverId(), driverId)) {
            throw new NotFoundException("Delivery not found: " + id);
        }
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