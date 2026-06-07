package id.co.lolita.laundry.order.adapter.in.web;

import id.co.lolita.laundry.order.adapter.in.web.dto.OrderFormResponse;
import id.co.lolita.laundry.order.adapter.in.web.dto.OrderResponse;
import id.co.lolita.laundry.order.adapter.in.web.dto.OrderLineRequest;
import id.co.lolita.laundry.order.adapter.in.web.dto.SubmitOrderRequest;
import id.co.lolita.laundry.order.domain.port.in.GetOrderFormUseCase;
import id.co.lolita.laundry.order.domain.port.in.SubmitPublicOrderUseCase;
import id.co.lolita.laundry.order.domain.port.in.SubmitPublicOrderUseCase.SubmitPublicOrderCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Public, unauthenticated order endpoints. Hotel staff reach these via the client's
 * tokenized URL. Both paths are permitted in {@code SecurityConfig} and rate-limited by
 * {@link PublicRateLimitInterceptor}.
 */
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
class PublicOrderController {

    private final GetOrderFormUseCase orderForm;
    private final SubmitPublicOrderUseCase submitOrder;

    @GetMapping("/order-form/{token}")
    OrderFormResponse getForm(@PathVariable UUID token) {
        return OrderFormResponse.from(orderForm.getPublicOrderForm(token));
    }

    @PostMapping("/orders/{token}")
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse submit(@PathVariable UUID token, @Valid @RequestBody SubmitOrderRequest request) {
        var command = new SubmitPublicOrderCommand(
                token, request.submittedByName(), request.treatment(), request.notes(),
                request.items().stream().map(OrderLineRequest::toInput).toList());
        return OrderResponse.from(submitOrder.submit(command));
    }
}
