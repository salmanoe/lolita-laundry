package id.co.lolita.laundry.order.application;

import id.co.lolita.laundry.order.domain.Order;
import id.co.lolita.laundry.order.domain.OrderLineItem;
import id.co.lolita.laundry.order.domain.OrderStatus;
import id.co.lolita.laundry.order.domain.event.OrderBillingSyncEvent;
import id.co.lolita.laundry.order.domain.event.OrderDeliveredEvent;
import id.co.lolita.laundry.order.domain.port.in.CancelOrderUseCase;
import id.co.lolita.laundry.order.domain.port.in.CreateOrderUseCase.CreateOrderCommand;
import id.co.lolita.laundry.order.domain.port.in.DeliverOrderUseCase.DeliverOrderCommand;
import id.co.lolita.laundry.order.domain.port.in.OrderLineInput;
import id.co.lolita.laundry.order.domain.port.in.UpdateOrderStatusUseCase.AdvanceStatusCommand;
import id.co.lolita.laundry.order.domain.port.in.UpdateOrderUseCase;
import id.co.lolita.laundry.order.domain.port.out.CatalogGateway;
import id.co.lolita.laundry.order.domain.port.out.ClientGateway;
import id.co.lolita.laundry.order.domain.port.out.ClientGateway.ClientSnapshot;
import id.co.lolita.laundry.order.domain.port.out.DeliveryConfirmationRepository;
import id.co.lolita.laundry.order.domain.port.out.OrderRepository;
import id.co.lolita.laundry.order.domain.port.out.OrderStatusHistoryRepository;
import id.co.lolita.laundry.order.domain.port.out.PhotoStoragePort;
import id.co.lolita.laundry.order.domain.port.out.PricingGateway;
import id.co.lolita.laundry.order.domain.port.out.billing.BillingStatusPort;
import id.co.lolita.laundry.shared.ConflictException;
import id.co.lolita.laundry.shared.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orchestration rules of OrderService that aren't visible in the domain object: order
 * number generation, price snapshotting, treatment gating, line-level department resolution,
 * the DELIVERED guard on status advancement, and delivery preconditions. Pure Mockito.
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
    PricingGateway pricingGateway;
    @Mock
    CatalogGateway catalogGateway;
    @Mock
    PhotoStoragePort photoStorage;
    @Mock
    BillingStatusPort billingStatus;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @Mock
    ObjectProvider<OrderService> self;
    @InjectMocks
    OrderService service;

    @BeforeEach
    void wireSelf() {
        // The creation paths re-enter via the self proxy (order-number retry); outside Spring the
        // proxy is just the service itself. lenient — most tests don't exercise the creation path.
        lenient().when(self.getObject()).thenReturn(service);
    }

    private ClientSnapshot client(boolean perDepartment) {
        return new ClientSnapshot(1L, "Are You and I", "AYI", true, perDepartment);
    }

    private Order persisted(OrderStatus status) {
        return new Order(99L, "AYI-20260101-001", 1L, LocalDate.now(), null, status,
                BigDecimal.ONE, "Staff", null, null, Instant.now(),
                List.of(new OrderLineItem(
                        500L, 10L, BigDecimal.ONE, new BigDecimal("5000"), new BigDecimal("5000.00"), null)));
    }

    private void stubPricingForItem10() {
        when(catalogGateway.findActiveById(10L))
                .thenReturn(Optional.of(new CatalogGateway.CatalogItem(10L, "Sheet King", 1L, "Pcs")));
        when(pricingGateway.effectivePrice(eq(1L), eq(10L), any()))
                .thenReturn(Optional.of(new BigDecimal("5000")));
    }

    @Test
    void createOrder_generatesSequentialOrderNumber_andSnapshotsPrice() {
        when(clientGateway.findById(1L)).thenReturn(Optional.of(client(false)));
        stubPricingForItem10();
        when(orderRepository.countByClientIdAndOrderDate(eq(1L), any())).thenReturn(0L);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createOrder(new CreateOrderCommand(
                1L, false, null, "Budi", null, null,
                List.of(new OrderLineInput(10L, new BigDecimal("3")))));

        var expected = "AYI-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001";
        assertThat(result.getOrderNumber()).isEqualTo(expected);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(result.getLineItems().getFirst().priceAtOrder()).isEqualByComparingTo("5000");
        assertThat(result.getLineItems().getFirst().subtotal()).isEqualByComparingTo("15000.00");
        assertThat(result.getLineItems().getFirst().departmentId()).isNull();   // COMBINED client
        verify(historyRepository).save(any());
    }

    @Test
    void createOrder_rejectsInactiveClient() {
        when(clientGateway.findById(1L))
                .thenReturn(Optional.of(new ClientSnapshot(1L, "X", "X", false, false)));

        assertThatThrownBy(() -> service.createOrder(new CreateOrderCommand(
                1L, false, null, "Budi", null, null, List.of(new OrderLineInput(10L, BigDecimal.ONE)))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_rejectsTreatmentForCombinedClient() {
        when(clientGateway.findById(1L)).thenReturn(Optional.of(client(false)));

        assertThatThrownBy(() -> service.createOrder(new CreateOrderCommand(
                1L, true, null, "Budi", null, null, List.of(new OrderLineInput(10L, BigDecimal.ONE)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Treatment");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_rejectsItemWithoutDepartmentForPerDepartmentClient() {
        when(clientGateway.findById(1L)).thenReturn(Optional.of(client(true)));
        when(catalogGateway.findActiveById(10L))
                .thenReturn(Optional.of(new CatalogGateway.CatalogItem(10L, "Sheet King", 1L, "Pcs")));
        when(pricingGateway.effectivePrice(eq(1L), eq(10L), any()))
                .thenReturn(Optional.of(new BigDecimal("5000")));
        when(pricingGateway.departmentForItem(1L, 10L)).thenReturn(Optional.empty());   // not assigned

        assertThatThrownBy(() -> service.createOrder(new CreateOrderCommand(
                1L, false, null, "Budi", null, null, List.of(new OrderLineInput(10L, BigDecimal.ONE)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("department");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_appliesTreatmentMultiplier_andStampsLineDepartment_forPerDepartmentClient() {
        when(clientGateway.findById(1L)).thenReturn(Optional.of(client(true)));
        stubPricingForItem10();
        when(pricingGateway.departmentForItem(1L, 10L)).thenReturn(Optional.of(7L));
        when(orderRepository.countByClientIdAndOrderDate(eq(1L), any())).thenReturn(0L);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createOrder(new CreateOrderCommand(
                1L, true, null, "Budi", null, null, List.of(new OrderLineInput(10L, new BigDecimal("3")))));

        assertThat(result.getPricingMultiplier()).isEqualByComparingTo("2.0");
        assertThat(result.getLineItems().getFirst().subtotal()).isEqualByComparingTo("30000.00");
        assertThat(result.getLineItems().getFirst().departmentId()).isEqualTo(7L);
    }

    @Test
    void createOrder_rejectsUnknownClient() {
        when(clientGateway.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(new CreateOrderCommand(
                42L, false, null, "Staff", null, null,
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
    void deliver_rejectedWhenAlreadyDelivered() {
        // Concurrency backstop: a second driver confirming an already-delivered order fails cleanly
        // with a friendly 409 (ConflictException) rather than a raw error.
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.DELIVERED)));

        assertThatThrownBy(() -> service.deliver(new DeliverOrderCommand(
                99L, "Rina", "Joko", null, new byte[]{1, 2}, "image/jpeg", "p.jpg", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("sudah dikirim");
        verify(photoStorage, never()).store(any(), any(), any());
    }

    @Test
    void deliver_translatesConcurrentConfirmCollisionTo409_andStoresNoOrphanPhoto() {
        // Two drivers pass the in-memory guard before either commits; the loser fails on
        // UNIQUE(delivery_confirmations.order_id). It must surface as a friendly 409, and the
        // photo must NOT have been written (store happens only after a successful save — KI-2).
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.DONE)));
        when(deliveryRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate order_id"));

        assertThatThrownBy(() -> service.deliver(new DeliverOrderCommand(
                99L, "Rina", "Joko", null, new byte[]{1, 2, 3}, "image/jpeg", "proof.jpg", 5L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("sudah dikirim");
        verify(photoStorage, never()).store(any(), any(), any());
        verify(orderRepository, never()).save(any());   // order status never persisted
    }

    @Test
    void createOrder_retriesOnceWhenOrderNumberCollides() {
        // A concurrent submit grabbed the same computed order_number: the first save fails on
        // UNIQUE(order_number); the retry recomputes the sequence and succeeds.
        when(clientGateway.findById(1L)).thenReturn(Optional.of(client(false)));
        stubPricingForItem10();
        when(orderRepository.countByClientIdAndOrderDate(eq(1L), any()))
                .thenReturn(0L)   // first attempt → -001
                .thenReturn(1L);  // retry sees the winner committed → -002
        when(orderRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate order_number"))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = service.createOrder(new CreateOrderCommand(
                1L, false, null, "Budi", null, null, List.of(new OrderLineInput(10L, new BigDecimal("3")))));

        var expected = "AYI-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-002";
        assertThat(result.getOrderNumber()).isEqualTo(expected);
        verify(orderRepository, times(2)).save(any());
    }

    @Test
    void createOrder_retriesAcrossSeveralCollisions_thenSucceeds() {
        // A runtime burst showed a single retry is too thin; the loop tolerates several rounds.
        when(clientGateway.findById(1L)).thenReturn(Optional.of(client(false)));
        stubPricingForItem10();
        when(orderRepository.countByClientIdAndOrderDate(eq(1L), any()))
                .thenReturn(0L, 1L, 2L, 3L);
        when(orderRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("dup"))
                .thenThrow(new DataIntegrityViolationException("dup"))
                .thenThrow(new DataIntegrityViolationException("dup"))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = service.createOrder(new CreateOrderCommand(
                1L, false, null, "Budi", null, null, List.of(new OrderLineInput(10L, new BigDecimal("1")))));

        var expected = "AYI-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-004";
        assertThat(result.getOrderNumber()).isEqualTo(expected);
        verify(orderRepository, times(4)).save(any());
    }

    @Test
    void createOrder_exhaustsRetries_throwsFriendlyConflict() {
        // Every attempt collides → a friendly 409, never the raw DB error or a 500.
        when(clientGateway.findById(1L)).thenReturn(Optional.of(client(false)));
        stubPricingForItem10();
        when(orderRepository.countByClientIdAndOrderDate(eq(1L), any())).thenReturn(0L);
        when(orderRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.createOrder(new CreateOrderCommand(
                1L, false, null, "Budi", null, null, List.of(new OrderLineInput(10L, BigDecimal.ONE)))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("coba lagi");
        verify(orderRepository, times(8)).save(any());   // MAX_ORDER_NUMBER_ATTEMPTS
    }

    @Test
    void deliver_allowedFromReceived_autoStampsSkippedSteps() {
        // Staff never advanced the order — delivery is still allowed and backfills the skipped steps.
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.RECEIVED)));
        when(photoStorage.store(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deliver(new DeliverOrderCommand(
                99L, "Rina", "Joko", null, new byte[]{1, 2, 3}, "image/jpeg", "proof.jpg", 5L));

        // RECEIVED → PROCESSING → DONE → DELIVERED = three auto-stamped history entries.
        verify(historyRepository, times(3)).save(any());
        verify(eventPublisher).publishEvent(any(OrderDeliveredEvent.class));
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
        // Billing reacts to this event to generate the order invoice.
        verify(eventPublisher).publishEvent(any(OrderDeliveredEvent.class));
    }

    @Test
    void deliver_sanitizesPhotoKeyAgainstACraftedFilename() {
        // KI-10: a non-image content type makes extensionFor fall back to the client filename's
        // suffix. A crafted name with path separators must NOT leak into the storage key — the
        // extension is whitelisted, so anything unrecognized defaults to .jpg.
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.DONE)));
        when(photoStorage.store(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var confirmation = service.deliver(new DeliverOrderCommand(
                99L, "Rina", "Joko", null, new byte[]{1, 2, 3},
                "application/octet-stream", "evil.php/../../etc/passwd", 5L));

        assertThat(confirmation.getPhotoUrl()).isEqualTo("photos/AYI-20260101-001.jpg");
        verify(photoStorage).store(eq("photos/AYI-20260101-001.jpg"), any(), eq("application/octet-stream"));
    }

    @Test
    void deliver_usesKnownImageExtensionFromFilenameWhenContentTypeMissing() {
        // A missing content type with a recognized image suffix is honored (whitelisted).
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.DONE)));
        when(photoStorage.store(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var confirmation = service.deliver(new DeliverOrderCommand(
                99L, "Rina", "Joko", null, new byte[]{1, 2, 3}, null, "proof.PNG", 5L));

        assertThat(confirmation.getPhotoUrl()).isEqualTo("photos/AYI-20260101-001.png");
    }

    @Test
    void cancel_setsCancelled_recordsHistory_andFiresBillingSync() {
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.RECEIVED)));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.cancel(new CancelOrderUseCase.CancelOrderCommand(99L, 5L, "duplikat"));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(historyRepository).save(any());
        // Billing reacts to drop the canceled order from the bill.
        verify(eventPublisher).publishEvent(any(OrderBillingSyncEvent.class));
    }

    @Test
    void updateOrder_treatmentCorrection_reprisesLines_andFiresBillingSync() {
        // SUPER_ADMIN flips Reguler → Treatment on a per-department client's RECEIVED order.
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.RECEIVED)));
        when(clientGateway.findById(1L)).thenReturn(Optional.of(client(true)));
        when(billingStatus.isOrderOnIssuedBilling(99L)).thenReturn(false);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateOrder(new UpdateOrderUseCase.UpdateOrderCommand(
                99L, null, true, null, null, null));

        assertThat(result.getPricingMultiplier()).isEqualByComparingTo("2.0");
        // Existing line (qty 1 × 5000) re-priced ×2, price snapshot untouched.
        assertThat(result.getLineItems().getFirst().priceAtOrder()).isEqualByComparingTo("5000");
        assertThat(result.getLineItems().getFirst().subtotal()).isEqualByComparingTo("10000.00");
        verify(eventPublisher).publishEvent(any(OrderBillingSyncEvent.class));
    }

    @Test
    void updateOrder_treatmentCorrection_rejectedWhenOnIssuedBilling() {
        // The invoice already went to the client — a treatment change must not re-total it.
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.RECEIVED)));
        when(clientGateway.findById(1L)).thenReturn(Optional.of(client(true)));
        when(billingStatus.isOrderOnIssuedBilling(99L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateOrder(new UpdateOrderUseCase.UpdateOrderCommand(
                99L, null, true, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("treatment");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrder_rejectsTreatmentForCombinedClient() {
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.RECEIVED)));
        when(clientGateway.findById(1L)).thenReturn(Optional.of(client(false)));

        assertThatThrownBy(() -> service.updateOrder(new UpdateOrderUseCase.UpdateOrderCommand(
                99L, null, true, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Treatment");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancel_rejectedWhenDelivered() {
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.DELIVERED)));

        assertThatThrownBy(() -> service.cancel(new CancelOrderUseCase.CancelOrderCommand(99L, 5L, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deliver_rejectedWhenCancelled() {
        when(orderRepository.findById(99L)).thenReturn(Optional.of(persisted(OrderStatus.CANCELLED)));

        assertThatThrownBy(() -> service.deliver(new DeliverOrderCommand(
                99L, "Rina", "Joko", null, new byte[]{1, 2}, "image/jpeg", "p.jpg", null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(photoStorage, never()).store(any(), any(), any());
    }

    @Test
    void getOpenDeliveries_mapsToPriceFreeView() {
        when(orderRepository.findOpenDeliveries()).thenReturn(List.of(persisted(OrderStatus.DONE)));
        when(clientGateway.findById(1L)).thenReturn(Optional.of(client(false)));
        when(catalogGateway.findActiveById(10L))
                .thenReturn(Optional.of(new CatalogGateway.CatalogItem(10L, "Sheet King", 1L, "Pcs")));

        var views = service.getOpenDeliveries();

        assertThat(views).singleElement().satisfies(v -> {
            assertThat(v.orderNumber()).isEqualTo("AYI-20260101-001");
            assertThat(v.clientName()).isEqualTo("Are You and I");
            assertThat(v.lines()).singleElement().satisfies(l -> {
                assertThat(l.itemName()).isEqualTo("Sheet King");
                assertThat(l.unitName()).isEqualTo("Pcs");
                assertThat(l.quantity()).isEqualByComparingTo("1");
            });
        });
    }

    @Test
    void getOrderForm_showsOnlyTheClientsPricedItems_withDepartment() {
        when(clientGateway.findById(1L)).thenReturn(Optional.of(client(false)));
        // Client has a price only for item 10; item 20 is active but unpriced for this client.
        when(pricingGateway.currentPrices(1L)).thenReturn(List.of(
                new PricingGateway.ItemPrice(10L, new BigDecimal("5000"))));
        when(pricingGateway.itemDepartments(1L)).thenReturn(List.of());   // COMBINED — no mappings
        when(catalogGateway.activeItems()).thenReturn(List.of(
                new CatalogGateway.CatalogItem(10L, "Sheet King", 1L, "Pcs"),
                new CatalogGateway.CatalogItem(20L, "Bath Towel", 2L, "Lembar")));

        var view = service.getOrderForm(1L);

        assertThat(view.items()).singleElement().satisfies(item -> {
            assertThat(item.itemId()).isEqualTo(10L);          // unpriced item 20 is excluded
            assertThat(item.name()).isEqualTo("Sheet King");
            assertThat(item.unitName()).isEqualTo("Pcs");
            assertThat(item.departmentId()).isNull();          // COMBINED client
        });
    }
}
