package id.co.lolita.laundry.order.application;

import id.co.lolita.laundry.order.domain.*;
import id.co.lolita.laundry.order.domain.event.OrderBillingSyncEvent;
import id.co.lolita.laundry.order.domain.event.OrderDeliveredEvent;
import id.co.lolita.laundry.order.domain.port.in.*;
import id.co.lolita.laundry.order.domain.port.out.*;
import id.co.lolita.laundry.order.domain.port.out.ClientGateway.ClientSnapshot;
import id.co.lolita.laundry.shared.NotFoundException;
import id.co.lolita.laundry.shared.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class OrderService implements GetOrderFormUseCase, SubmitPublicOrderUseCase, CreateOrderUseCase,
        GetOrdersUseCase, UpdateOrderUseCase, UpdateOrderStatusUseCase, CancelOrderUseCase,
        DeliverOrderUseCase, GetDriverDeliveriesUseCase, DeliveredOrderQuery {

    private static final BigDecimal TREATMENT_MULTIPLIER = new BigDecimal("2.0");
    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int PHOTO_URL_TTL_SECONDS = 900;   // 15 min — long enough to view, short enough to not leak

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final DeliveryConfirmationRepository deliveryRepository;
    private final ClientGateway clientGateway;
    private final DepartmentGateway departmentGateway;
    private final PricingGateway pricingGateway;
    private final CatalogGateway catalogGateway;
    private final PhotoStoragePort photoStorage;
    private final ApplicationEventPublisher eventPublisher;

    // ── GetOrderFormUseCase ──

    @Override
    public OrderFormView getPublicOrderForm(UUID token) {
        var client = activeClientByToken(token);

        List<OrderFormView.DepartmentLine> departments = client.perDepartment()
                ? departmentGateway.activeForClient(client.id()).stream()
                .map(d -> new OrderFormView.DepartmentLine(d.id(), d.name())).toList()
                : List.of();

        Map<Long, BigDecimal> prices = pricingGateway.currentPrices(client.id()).stream()
                .collect(Collectors.toMap(PricingGateway.ItemPrice::itemId, PricingGateway.ItemPrice::pricePerUnit));

        // Only items the client has a price for (its Daftar Harga) appear on the order form —
        // an unpriced item can't be ordered (order creation rejects it), so don't show it. The
        // price value is used only to filter; it is never exposed on the public form.
        List<OrderFormView.ItemLine> items = catalogGateway.activeItems().stream()
                .filter(it -> prices.containsKey(it.id()))
                .map(it -> new OrderFormView.ItemLine(
                        it.id(), it.name(), it.unitId(), it.unitName(),
                        it.categoryId(), it.categoryName()))
                .toList();

        return new OrderFormView(client.id(), client.name(), client.clientCode(),
                client.perDepartment(), client.perDepartment(), departments, items);
    }

    // ── SubmitPublicOrderUseCase ──

    @Override
    @Transactional
    public Order submit(SubmitPublicOrderCommand command) {
        requireName(command.submittedByName());
        var client = activeClientByToken(command.token());
        return assemble(client, command.departmentId(), command.treatment(), null,
                command.submittedByName(), command.notes(), null, command.items());
    }

    // ── CreateOrderUseCase ──

    @Override
    @Transactional
    public Order createOrder(CreateOrderCommand command) {
        requireName(command.submittedByName());
        var client = clientGateway.findById(command.clientId())
                .orElseThrow(() -> new NotFoundException("Client not found: " + command.clientId()));
        if (!client.active()) {
            throw new IllegalArgumentException("Client is inactive: " + command.clientId());
        }
        return assemble(client, command.departmentId(), command.treatment(), command.dueDate(),
                command.submittedByName(), command.notes(), command.createdByUserId(), command.items());
    }

    // ── GetOrdersUseCase ──

    @Override
    public Page<Order> getOrders(OrderQuery query) {
        return orderRepository.findAll(query);
    }

    @Override
    public Order getById(Long id) {
        return loadOrder(id);
    }

    @Override
    public List<OrderStatusHistory> getHistory(Long orderId) {
        loadOrder(orderId);   // 404 if the order doesn't exist
        return historyRepository.findByOrderId(orderId);
    }

    @Override
    public Optional<DeliveryConfirmation> getDelivery(Long orderId) {
        loadOrder(orderId);
        return deliveryRepository.findByOrderId(orderId);
    }

    @Override
    public Optional<String> getDeliveryPhotoUrl(Long orderId) {
        loadOrder(orderId);
        return deliveryRepository.findByOrderId(orderId)
                .map(DeliveryConfirmation::getPhotoUrl)
                .filter(key -> !key.isBlank())
                .map(key -> photoStorage.presignedUrl(key, PHOTO_URL_TTL_SECONDS));
    }

    // ── UpdateOrderUseCase ──

    @Override
    @Transactional
    public Order updateOrder(UpdateOrderCommand command) {
        var order = loadOrder(command.orderId());
        List<Order.NewLine> lines = (command.items() == null || command.items().isEmpty())
                ? null
                : priceLines(order.getClientId(), order.getOrderDate(), command.items());
        order.edit(command.dueDate(), command.notes(), lines);
        var saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderBillingSyncEvent(saved.getId()));   // re-price the billing line
        return saved;
    }

    // ── CancelOrderUseCase ──

    @Override
    @Transactional
    public Order cancel(CancelOrderCommand command) {
        var order = loadOrder(command.orderId());
        var from = order.getStatus();
        order.cancel();
        var saved = orderRepository.save(order);
        historyRepository.save(OrderStatusHistory.record(
                saved.getId(), from, OrderStatus.CANCELLED, command.byUserId(),
                command.notes() == null || command.notes().isBlank() ? "Order dibatalkan" : command.notes(),
                Instant.now()));
        eventPublisher.publishEvent(new OrderBillingSyncEvent(saved.getId()));   // drop from the billing
        return saved;
    }

    // ── UpdateOrderStatusUseCase ──

    @Override
    @Transactional
    public Order advanceStatus(AdvanceStatusCommand command) {
        if (command.target() == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException(
                    "Use the delivery endpoint to mark an order DELIVERED");
        }
        var order = loadOrder(command.orderId());
        var from = order.getStatus();
        order.advanceStatus(command.target());
        var saved = orderRepository.save(order);
        historyRepository.save(OrderStatusHistory.record(
                saved.getId(), from, command.target(), command.byUserId(), command.notes(), Instant.now()));
        return saved;
    }

    // ── DeliverOrderUseCase ──

    @Override
    @Transactional
    public DeliveryConfirmation deliver(DeliverOrderCommand command) {
        var order = loadOrder(command.orderId());
        if (command.photo() == null || command.photo().length == 0) {
            throw new IllegalArgumentException("A delivery photo is required");
        }

        // Delivery is allowed from any status except already-DELIVERED: staff may forget to
        // advance through PROCESSING/DONE, but the laundry still physically gets delivered.
        // markDelivered() fails fast if a second driver confirms an already-delivered order.
        var from = order.getStatus();
        var skipped = from.pathToDelivered();   // intermediate steps + DELIVERED, captured before the move
        order.markDelivered();

        var key = "photos/" + order.getOrderNumber()
                + extensionFor(command.photoContentType(), command.photoFilename());
        var storedKey = photoStorage.store(key, command.photo(), command.photoContentType());

        var confirmation = DeliveryConfirmation.create(order.getId(), command.recipientName(),
                command.delivererName(), storedKey, command.notes(), Instant.now());
        var saved = deliveryRepository.save(confirmation);

        orderRepository.save(order);

        // Stamp the status history. If staff never advanced the order, autofill the skipped
        // intermediate steps so the trail reads coherently, all attributed to the delivering user.
        var instant = Instant.now();
        var prev = from;
        for (var next : skipped) {
            var note = next == OrderStatus.DELIVERED ? "Order delivered" : "Auto-completed at delivery";
            historyRepository.save(OrderStatusHistory.record(
                    order.getId(), prev, next, command.byUserId(), note, instant));
            prev = next;
        }

        // Notify billing to generate the per-order invoice. Published after the order is saved;
        // the Modulith JPA registry persists it so the invoice still gets produced if the
        // listener fails. The listener runs after this transaction commits.
        eventPublisher.publishEvent(new OrderDeliveredEvent(
                order.getId(), order.getOrderNumber(), order.getClientId(), saved.getDeliveredAt()));
        return saved;
    }

    // ── GetDriverDeliveriesUseCase ──

    @Override
    public List<DriverDeliveryView> getOpenDeliveries() {
        return orderRepository.findOpenDeliveries().stream()
                .map(this::toDriverView)
                .toList();
    }

    private DriverDeliveryView toDriverView(Order order) {
        String clientName = clientGateway.findById(order.getClientId())
                .map(ClientSnapshot::name).orElse("—");
        String departmentName = order.getDepartmentId() == null ? null
                : departmentGateway.activeForClient(order.getClientId()).stream()
                .filter(d -> d.id().equals(order.getDepartmentId()))
                .map(DepartmentGateway.DepartmentSnapshot::name)
                .findFirst().orElse(null);

        var lines = order.getLineItems().stream().map(li -> {
            var item = catalogGateway.findActiveById(li.getItemId());
            String name = item.map(CatalogGateway.CatalogItem::name).orElse("#" + li.getItemId());
            String unit = item.map(CatalogGateway.CatalogItem::unitName).orElse(null);
            return new DriverLine(name, unit, li.getQuantity());
        }).toList();

        return new DriverDeliveryView(order.getId(), order.getOrderNumber(), clientName, departmentName,
                order.getOrderDate(), order.getDueDate(), order.getStatus(), order.getNotes(), lines);
    }

    // ── DeliveredOrderQuery (consumed by billing) ──

    @Override
    public Optional<DeliveredOrderDetail> findDeliveredOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(this::toDeliveredDetail);
    }

    @Override
    public List<DeliveredOrderDetail> findDeliveredOrders(Long clientId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return orderRepository.findDeliveredByClientAndPeriod(
                        clientId, ym.atDay(1), ym.atEndOfMonth()).stream()
                .map(this::toDeliveredDetail)
                .toList();
    }

    @Override
    public Optional<DeliveredOrderDetail> findBillableOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(this::toDeliveredDetail);
    }

    @Override
    public List<DeliveredOrderDetail> findBillableOrders(Long clientId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return orderRepository.findBillableByClientAndPeriod(
                        clientId, ym.atDay(1), ym.atEndOfMonth()).stream()
                .map(this::toDeliveredDetail)
                .toList();
    }

    private DeliveredOrderDetail toDeliveredDetail(Order order) {
        String departmentName = order.getDepartmentId() == null ? null
                : departmentGateway.activeForClient(order.getClientId()).stream()
                .filter(d -> d.id().equals(order.getDepartmentId()))
                .map(DepartmentGateway.DepartmentSnapshot::name)
                .findFirst().orElse(null);

        var lines = order.getLineItems().stream().map(li -> {
            var item = catalogGateway.findActiveById(li.getItemId());
            String name = item.map(CatalogGateway.CatalogItem::name).orElse("#" + li.getItemId());
            String unit = item.map(CatalogGateway.CatalogItem::unitName).orElse(null);
            return new DeliveredLine(name, unit, li.getQuantity(), li.getPriceAtOrder(), li.getSubtotal());
        }).toList();

        return new DeliveredOrderDetail(order.getId(), order.getOrderNumber(), order.getClientId(),
                order.getDepartmentId(), departmentName, order.getOrderDate(),
                order.getPricingMultiplier(), order.total(), lines);
    }

    // ── helpers ──

    private Order assemble(ClientSnapshot client, Long departmentId, boolean treatment, LocalDate dueDate,
                           String submittedByName, String notes, Long createdByUserId, List<OrderLineInput> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        Long resolvedDepartmentId = resolveDepartment(client, departmentId);
        BigDecimal multiplier = resolveMultiplier(client, treatment);
        LocalDate orderDate = LocalDate.now();

        var lines = priceLines(client.id(), orderDate, items);
        var orderNumber = generateOrderNumber(client, orderDate);

        var order = Order.create(orderNumber, client.id(), resolvedDepartmentId, orderDate, dueDate,
                multiplier, submittedByName, notes, createdByUserId, lines, Instant.now());
        var saved = orderRepository.save(order);
        historyRepository.save(OrderStatusHistory.record(
                saved.getId(), null, OrderStatus.RECEIVED, createdByUserId, "Order created", saved.getCreatedAt()));
        eventPublisher.publishEvent(new OrderBillingSyncEvent(saved.getId()));   // auto-add to the month's billing
        return saved;
    }

    private Long resolveDepartment(ClientSnapshot client, Long departmentId) {
        if (!client.perDepartment()) {
            return null;   // departments are irrelevant for combined-billing clients
        }
        if (departmentId == null) {
            throw new IllegalArgumentException("A department is required for this client");
        }
        if (!departmentGateway.existsForClient(departmentId, client.id())) {
            throw new IllegalArgumentException("Department %d does not belong to client %d"
                    .formatted(departmentId, client.id()));
        }
        return departmentId;
    }

    private BigDecimal resolveMultiplier(ClientSnapshot client, boolean treatment) {
        if (!treatment) {
            return BigDecimal.ONE;
        }
        if (!client.perDepartment()) {
            throw new IllegalArgumentException("Treatment pricing is only available for per-department clients");
        }
        return TREATMENT_MULTIPLIER;
    }

    private List<Order.NewLine> priceLines(Long clientId, LocalDate orderDate, List<OrderLineInput> items) {
        return items.stream().map(in -> {
            if (in.itemId() == null) {
                throw new IllegalArgumentException("Each line must reference an item");
            }
            catalogGateway.findActiveById(in.itemId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown or inactive item: " + in.itemId()));
            var price = pricingGateway.effectivePrice(clientId, in.itemId(), orderDate)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No price set for item %d for this client".formatted(in.itemId())));
            return new Order.NewLine(in.itemId(), in.quantity(), price);
        }).toList();
    }

    private String generateOrderNumber(ClientSnapshot client, LocalDate date) {
        long seq = orderRepository.countByClientIdAndOrderDate(client.id(), date) + 1;
        return "%s-%s-%03d".formatted(client.clientCode(), date.format(ORDER_DATE_FORMAT), seq);
    }

    private ClientSnapshot activeClientByToken(UUID token) {
        return clientGateway.findByToken(token)
                .filter(ClientSnapshot::active)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive order token"));
    }

    private Order loadOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    private static void requireName(String submittedByName) {
        if (submittedByName == null || submittedByName.isBlank()) {
            throw new IllegalArgumentException("Nama Staff is required");
        }
    }

    private static String extensionFor(String contentType, String filename) {
        if (contentType != null) {
            if (contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/jpg")) {
                return ".jpg";
            }
            if (contentType.equalsIgnoreCase("image/png")) {
                return ".png";
            }
            if (contentType.equalsIgnoreCase("image/webp")) {
                return ".webp";
            }
        }
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0 && dot < filename.length() - 1) {
                return filename.substring(dot).toLowerCase();
            }
        }
        return ".jpg";
    }
}