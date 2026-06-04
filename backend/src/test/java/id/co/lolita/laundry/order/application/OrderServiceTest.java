package id.co.lolita.laundry.order.application;

import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderStatus;
import id.co.lolita.laundry.order.domain.port.in.CreateOrderUseCase.CreateOrderCommand;
import id.co.lolita.laundry.order.domain.port.in.DeliverOrderUseCase.DeliverOrderCommand;
import id.co.lolita.laundry.order.domain.port.in.OrderLineInput;
import id.co.lolita.laundry.order.domain.port.in.SubmitPublicOrderUseCase.SubmitPublicOrderCommand;
import id.co.lolita.laundry.order.domain.port.in.UpdateOrderStatusUseCase.AdvanceStatusCommand;
import id.co.lolita.laundry.order.domain.port.out.CatalogGateway;
import id.co.lolita.laundry.order.domain.port.out.ClientGateway;
import id.co.lolita.laundry.order.domain.port.out.ClientGateway.ClientSnapshot;
import id.co.lolita.laundry.order.domain.port.out.DeliveryConfirmationRepository;
import id.co.lolita.laundry.order.domain.port.out.DepartmentGateway;
import id.co.lolita.laundry.order.domain.port.out.OrderRepository;
import id.co.lolita.laundry.order.domain.port.out.OrderStatusHistoryRepository;
import id.co.lolita.laundry.order.domain.port.out.PhotoStoragePort;
import id.co.lolita.laundry.order.domain.port.out.PricingGateway;
import id.co.lolita.laundry.shared.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orchestration rules of OrderService that aren't visible in the domain object: order
 * number generation, price snapshotting, treatment gating, department requirement, the
 * DELIVERED guard on status advancement, and delivery preconditions. Pure Mockito.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    OrderStatusHistoryRepository historyRepository;
    @Mock
    DeliveryConfirmationRepository deliveryRepository;
    @Mock
    ClientGateway clientGateway;
    @Mock
    DepartmentGateway departmentGateway;
    @Mock
    PricingGateway pricingGateway;
    @Mock
    CatalogGateway catalogGateway;
    @Mock
    PhotoStoragePort photoStorage;
    @InjectMocks
    OrderService service;

    private static final UUID TOKEN = UUID.randomUUID();

    private ClientSnapshot client(boolean perDepartment) {
        return new ClientSnapshot(1L, "Are You and I", "AYI", true, perDepartment);
    }

    private Order persisted(OrderStatus status) {
        return new Order(99L, "AYI-20260101-001", 1L, null, LocalDate.now(), null, status,
                BigDecimal.ONE, "Staff", null, null, Instant.now(),
                List.of(new id.co.lolita.laundry.order.domain.OrderLineItem(
                        500L, 10L, BigDecimal.ONE, new BigDecimal("5000"), new BigDecimal("5000.00"))));
    }

    private void stubPricingForItem10() {
        when(catalogGateway.findActiveById(10L))
                .thenReturn(Optional.of(new CatalogGateway.CatalogItem(10L, "Sheet King", 1L, 1L)));
        when(pricingGateway.effectivePrice(eq(1L), eq(10L), any()))
                .thenReturn(Optional.of(new BigDecimal("5000")));
    }

    @Test
    void submit_generatesSequentialOrderNumber_andSnapshotsPrice() {
        when(clientGateway.findByToken(TOKEN)).thenReturn(Optional.of(client(false)));
        stubPricingForItem10();
        when(orderRepository.countByClientIdAndOrderDate(eq(1L), any())).thenReturn(0L);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.submit(new SubmitPublicOrderCommand(
                TOKEN, "Budi", null, false, null,
                List.of(new OrderLineInput(10L, new BigDecimal("3")))));

        var expected = "AYI-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001";
        assertThat(result.getOrderNumber()).isEqualTo(expected);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(result.getLineItems().getFirst().getPriceAtOrder()).isEqualByComparingTo("5000");
        assertThat(result.getLineItems().getFirst().getSubtotal()).isEqualByComparingTo("15000.00");
        verify(historyRepository).save(any());
    }

    @Test
    void submit_rejectsInactiveToken() {
        when(clientGateway.findByToken(TOKEN))
                .thenReturn(Optional.of(new ClientSnapshot(1L, "X", "X", false, false)));

        assertThatThrownBy(() -> service.submit(new SubmitPublicOrderCommand(
                TOKEN, "Budi", null, false, null, List.of(new OrderLineInput(10L, BigDecimal.ONE)))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void submit_rejectsTreatmentForCombinedClient() {
        when(clientGateway.findByToken(TOKEN)).thenReturn(Optional.of(client(false)));

        assertThatThrownBy(() -> service.submit(new SubmitPublicOrderCommand(
                TOKEN, "Budi", null, true, null, List.of(new OrderLineInput(10L, BigDecimal.ONE)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Treatment");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void submit_requiresDepartmentForPerDepartmentClient() {
        when(clientGateway.findByToken(TOKEN)).thenReturn(Optional.of(client(true)));

        assertThatThrownBy(() -> service.submit(new SubmitPublicOrderCommand(
                TOKEN, "Budi", null, false, null, List.of(new OrderLineInput(10L, BigDecimal.ONE)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("department");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void submit_appliesTreatmentMultiplier_forPerDepartmentClient() {
        when(clientGateway.findByToken(TOKEN)).thenReturn(Optional.of(client(true)));
        when(departmentGateway.existsForClient(7L, 1L)).thenReturn(true);
        stubPricingForItem10();
        when(orderRepository.countByClientIdAndOrderDate(eq(1L), any())).thenReturn(0L);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.submit(new SubmitPublicOrderCommand(
                TOKEN, "Budi", 7L, true, null, List.of(new OrderLineInput(10L, new BigDecimal("3")))));

        assertThat(result.getPricingMultiplier()).isEqualByComparingTo("2.0");
        assertThat(result.getLineItems().getFirst().getSubtotal()).isEqualByComparingTo("30000.00");
        assertThat(result.getDepartmentId()).isEqualTo(7L);
    }

    @Test
    void createOrder_rejectsUnknownClient() {
        when(clientGateway.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(new CreateOrderCommand(
                42L, null, false, null, "Staff", null, null,
                List.of(new OrderLineInput(10L, BigDecimal.ONE)))))
                .isInstanceOf(NotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void advanceStatus_rejectsDeliveredTarget() {
        assertThatThrownBy(() -> service.advanceStatus(
                new AdvanceStatusCommand(99L, OrderStatus.DELIVERED, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void advanceStatus_movesForwardAndRecordsHistory() {
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.RECEIVED)));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.advanceStatus(new AdvanceStatusCommand(99L, OrderStatus.PROCESSING, 5L, "ok"));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(historyRepository).save(any());
    }

    @Test
    void deliver_rejectedWhenNotDone() {
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.PROCESSING)));

        assertThatThrownBy(() -> service.deliver(new DeliverOrderCommand(
                99L, "Rina", "Joko", null, new byte[]{1, 2}, "image/jpeg", "p.jpg", null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(photoStorage, never()).store(any(), any(), any());
    }

    @Test
    void deliver_rejectedWithoutPhoto() {
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.DONE)));

        assertThatThrownBy(() -> service.deliver(new DeliverOrderCommand(
                99L, "Rina", "Joko", null, new byte[]{}, "image/jpeg", "p.jpg", null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(photoStorage, never()).store(any(), any(), any());
    }

    @Test
    void deliver_storesPhoto_marksDelivered_andRecordsHistory() {
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.DONE)));
        when(photoStorage.store(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var confirmation = service.deliver(new DeliverOrderCommand(
                99L, "Rina", "Joko", "left at lobby", new byte[]{1, 2, 3}, "image/jpeg", "proof.jpg", 5L));

        assertThat(confirmation.getRecipientName()).isEqualTo("Rina");
        assertThat(confirmation.getPhotoUrl()).isEqualTo("photos/AYI-20260101-001.jpg");
        verify(photoStorage).store(eq("photos/AYI-20260101-001.jpg"), any(), eq("image/jpeg"));
        verify(orderRepository).save(any());     // order advanced to DELIVER
        verify(historyRepository).save(any());
    }
}
