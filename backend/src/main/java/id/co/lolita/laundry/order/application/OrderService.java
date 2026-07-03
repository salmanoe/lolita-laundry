package id.co.lolita.laundry.order.application;

import id.co.lolita.laundry.order.domain.*;
import id.co.lolita.laundry.order.domain.event.OrderBillingSyncEvent;
import id.co.lolita.laundry.order.domain.event.OrderDeliveredEvent;
import id.co.lolita.laundry.order.domain.port.in.*;
import id.co.lolita.laundry.order.domain.port.out.*;
import id.co.lolita.laundry.order.domain.port.out.ClientGateway.ClientSnapshot;
import id.co.lolita.laundry.order.domain.port.out.billing.BillingStatusPort;
import id.co.lolita.laundry.shared.ConflictException;
import id.co.lolita.laundry.shared.NotFoundException;
import id.co.lolita.laundry.shared.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class OrderService implements GetOrderFormUseCase, CreateOrderUseCase,
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
    private final BillingStatusPort billingStatus;
    private final ApplicationEventPublisher eventPublisher;
    // Self-reference so the order-number retry can re-enter a fresh transaction per attempt
    // (a plain this.* call would bypass the @Transactional proxy). Lazy → no init cycle.
    private final ObjectProvider<OrderService> self;

    // In-JVM serialization of order creation per client (single-VM deployment). Holding this lock
    // across the creation transaction means each submit reads the per-day sequence only after the
    // previous one committed → no order_number collision in practice. The UNIQUE constraint + the
    // bounded retry below remain the cross-instance / paranoia backstop. (final + initializer →
    // excluded from the @RequiredArgsConstructor.)
    private final ConcurrentHashMap<String, ReentrantLock> orderCreationLocks = new ConcurrentHashMap<>();

    // ── GetOrderFormUseCase ──

    @Override
    public OrderFormView getOrderForm(Long clientId) {
        var client = clientGateway.findById(clientId)
                .filter(ClientSnapshot::active)
                .orElseThrow(() -> new NotFoundException("Client not found or inactive: " + clientId));

        List<OrderFormView.DepartmentLine> departments = client.perDepartment()
                ? departmentGateway.activeForClient(client.id()).stream()
                .map(d -> new OrderFormView.DepartmentLine(d.id(), d.name())).toList()
                : List.of();

        Map<Long, BigDecimal> prices = pricingGateway.currentPrices(client.id()).stream()
                .collect(Collectors.toMap(PricingGateway.ItemPrice::itemId, PricingGateway.ItemPrice::pricePerUnit));

        // Item→department assignments (PER_DEPARTMENT clients only) — the public form groups
        // items by department on this.
        Map<Long, Long> itemDepartments = pricingGateway.itemDepartments(client.id()).stream()
                .collect(Collectors.toMap(PricingGateway.ItemDepartment::itemId,
                        PricingGateway.ItemDepartment::departmentId));

        // Only items the client has a price for (its Daftar Harga) appear on the order form —
        // an unpriced item can't be ordered (order creation rejects it), so don't show it. The
        // price value is used only to filter; it is never exposed on the public form.
        List<OrderFormView.ItemLine> items = catalogGateway.activeItems().stream()
                .filter(it -> prices.containsKey(it.id()))
                .map(it -> new OrderFormView.ItemLine(
                        it.id(), it.name(), it.unitId(), it.unitName(),
                        itemDepartments.get(it.id())))
                .toList();

        return new OrderFormView(client.id(), client.name(), client.clientCode(),
                client.perDepartment(), client.perDepartment(), departments, items);
    }

    // ── CreateOrderUseCase ──

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Order createOrder(CreateOrderCommand command) {
        requireName(command.submittedByName());
        return serializedPerClient("cli:" + command.clientId(),
                () -> withOrderNumberRetry(() -> self.getObject().createOrderInTx(command)));
    }

    @Transactional
    public Order createOrderInTx(CreateOrderCommand command) {
        var client = clientGateway.findById(command.clientId())
                .orElseThrow(() -> new NotFoundException("Client not found: " + command.clientId()));
        if (!client.active()) {
            throw new IllegalArgumentException("Client is inactive: " + command.clientId());
        }
        return assemble(client, command.treatment(), command.dueDate(),
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
        var client = clientGateway.findById(order.getClientId());
        boolean perDepartment = client.map(ClientSnapshot::perDepartment).orElse(false);

        // SUPER_ADMIN Treatment correction. resolveMultiplier enforces the PER_DEPARTMENT-only gate
        // (Treatment on a COMBINED client is rejected). null = leave the flag unchanged.
        BigDecimal newMultiplier = command.treatment() == null ? null
                : client.map(c -> resolveMultiplier(c, command.treatment()))
                .orElseThrow(() -> new IllegalArgumentException("Client not found for order " + order.getId()));

        // SUPER_ADMIN order-date / Treatment corrections. Rejected once the order sits on an
        // ISSUED/PAID billing: that invoice has already gone to the client and must not be
        // retroactively moved to another period or re-totalled. (Controller gates both to SUPER_ADMIN;
        // only reached when the value actually changes.)
        boolean dateChanged = command.orderDate() != null && !command.orderDate().equals(order.getOrderDate());
        boolean treatmentChanged = newMultiplier != null
                && newMultiplier.compareTo(order.getPricingMultiplier()) != 0;
        if ((dateChanged || treatmentChanged) && billingStatus.isOrderOnIssuedBilling(order.getId())) {
            throw new IllegalArgumentException(dateChanged
                    ? "Tanggal order tidak dapat diubah karena order sudah masuk tagihan yang telah diterbitkan."
                    : "Status treatment tidak dapat diubah karena order sudah masuk tagihan yang telah diterbitkan.");
        }

        // Price any supplied line items at the effective order date (the new date if it changed),
        // so a re-submitted item list reflects the price list for that date. A date-only edit leaves
        // the existing lines — and their frozen price snapshots — untouched. A Treatment-only change
        // re-prices the existing lines inside Order.edit with the corrected multiplier.
        var effectiveDate = command.orderDate() != null ? command.orderDate() : order.getOrderDate();
        List<Order.NewLine> lines = null;
        if (command.items() != null && !command.items().isEmpty()) {
            lines = priceLines(order.getClientId(), perDepartment, effectiveDate, command.items());
        }
        order.edit(command.orderDate(), newMultiplier, command.dueDate(), command.notes(), lines);
        var saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderBillingSyncEvent(saved.getId()));   // re-price / re-home the billing line
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
        // A sequential second confirm sees DELIVERED here → friendly 409 (not a raw 400/500).
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new ConflictException("Order sudah dikirim");
        }
        var from = order.getStatus();
        var skipped = from.pathToDelivered();   // intermediate steps + DELIVERED, captured before the move
        order.markDelivered();

        // Persist the confirmation first: UNIQUE(delivery_confirmations.order_id) is the real
        // concurrency backstop — two drivers can both pass the in-memory guard above before
        // either commits, and the second insert is what actually loses. Storing the photo only
        // after a successful save avoids leaving an orphan object in storage (KI-2).
        var key = "photos/" + order.getOrderNumber()
                + extensionFor(command.photoContentType(), command.photoFilename());
        var confirmation = DeliveryConfirmation.create(order.getId(), command.recipientName(),
                command.delivererName(), key, command.notes(), Instant.now());
        DeliveryConfirmation saved;
        try {
            saved = deliveryRepository.save(confirmation);
        } catch (DataIntegrityViolationException duplicate) {
            // A concurrent driver confirmed this order first (IDENTITY id → the INSERT and its
            // unique-constraint check happen at save time, so we catch it here).
            throw new ConflictException("Order sudah dikirim");
        }

        photoStorage.store(key, command.photo(), command.photoContentType());

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

        var lines = order.getLineItems().stream().map(li -> {
            var item = catalogGateway.findActiveById(li.itemId());
            String name = item.map(CatalogGateway.CatalogItem::name).orElse("#" + li.itemId());
            String unit = item.map(CatalogGateway.CatalogItem::unitName).orElse(null);
            return new DriverLine(name, unit, li.quantity());
        }).toList();

        return new DriverDeliveryView(order.getId(), order.getOrderNumber(), clientName,
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
        // Resolve department display names once for the order's client (PER_DEPARTMENT only).
        // Use all departments (incl. inactive): these are historical labels for an existing order,
        // so a department deactivated after the order was placed must still resolve its name (KI-5).
        Map<Long, String> departmentNames = order.getLineItems().stream().anyMatch(li -> li.departmentId() != null)
                ? departmentGateway.allForClient(order.getClientId()).stream()
                .collect(Collectors.toMap(DepartmentGateway.DepartmentSnapshot::id,
                        DepartmentGateway.DepartmentSnapshot::name))
                : Map.of();

        var lines = order.getLineItems().stream().map(li -> {
            var item = catalogGateway.findActiveById(li.itemId());
            String name = item.map(CatalogGateway.CatalogItem::name).orElse("#" + li.itemId());
            String unit = item.map(CatalogGateway.CatalogItem::unitName).orElse(null);
            String deptName = li.departmentId() == null ? null : departmentNames.get(li.departmentId());
            return new DeliveredLine(name, unit, li.quantity(), li.priceAtOrder(), li.subtotal(),
                    li.departmentId(), deptName);
        }).toList();

        return new DeliveredOrderDetail(order.getId(), order.getOrderNumber(), order.getClientId(),
                order.getOrderDate(), order.getPricingMultiplier(), order.total(),
                order.getStatus() == OrderStatus.DELIVERED, lines);
    }

    // ── helpers ──

    /**
     * Serializes order creation for one client (single-VM deployment) so concurrent submits don't
     * compute the same {@code order_number}. The lock is held across the whole create — including
     * the inner transaction's commit — so the next waiter reads the per-day sequence only after
     * the previous order is visible.
     */
    private <T> T serializedPerClient(String key, Supplier<T> work) {
        var lock = orderCreationLocks.computeIfAbsent(key, _ -> new ReentrantLock());
        lock.lock();
        try {
            return work.get();
        } finally {
            lock.unlock();
        }
    }

    // Cross-instance / paranoia backstop only — the per-client lock above prevents in-JVM
    // collisions, so this rarely runs. A generous bound still terminates a pathological storm.
    private static final int MAX_ORDER_NUMBER_ATTEMPTS = 8;

    /**
     * Runs an order-creating attempt, retrying on an {@code order_number} collision with a
     * concurrent submit. Submits for the same client on the same day each compute
     * {@code seq = count + 1} → identical number; {@code UNIQUE(orders.order_number)} makes the
     * loser fail with a {@link DataIntegrityViolationException}. Re-running recomputes the sequence
     * (winners are now committed and counted) so each retried order gets the next free number.
     * Each attempt runs in its own transaction (the caller is {@code NOT_SUPPORTED}), so a failed
     * attempt's rollback is fully released before the retry re-reads the count. A runtime burst
     * showed a single retry is too thin under heavy contention, so we loop a bounded number of
     * times and, only if every round still collides, surface a friendly 409 instead of the raw
     * DB error.
     */
    private Order withOrderNumberRetry(Supplier<Order> attempt) {
        for (int remaining = MAX_ORDER_NUMBER_ATTEMPTS; remaining > 1; remaining--) {
            try {
                return attempt.get();
            } catch (DataIntegrityViolationException collision) {
                // a concurrent submit took our number — recompute the sequence and retry
            }
        }
        try {
            return attempt.get();   // last attempt — let a non-collision error propagate as-is
        } catch (DataIntegrityViolationException exhausted) {
            throw new ConflictException("Order bersamaan sedang ramai, nomor order bentrok. Silakan coba lagi.");
        }
    }

    private Order assemble(ClientSnapshot client, boolean treatment, LocalDate dueDate,
                           String submittedByName, String notes, Long createdByUserId, List<OrderLineInput> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        BigDecimal multiplier = resolveMultiplier(client, treatment);
        LocalDate orderDate = LocalDate.now();

        var lines = priceLines(client.id(), client.perDepartment(), orderDate, items);
        var orderNumber = generateOrderNumber(client, orderDate);

        var order = Order.create(orderNumber, client.id(), orderDate, dueDate,
                multiplier, submittedByName, notes, createdByUserId, lines, Instant.now());
        var saved = orderRepository.save(order);
        historyRepository.save(OrderStatusHistory.record(
                saved.getId(), null, OrderStatus.RECEIVED, createdByUserId, "Order created", saved.getCreatedAt()));
        eventPublisher.publishEvent(new OrderBillingSyncEvent(saved.getId()));   // auto-add to the month's billing
        return saved;
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

    private List<Order.NewLine> priceLines(Long clientId, boolean perDepartment, LocalDate orderDate,
                                           List<OrderLineInput> items) {
        return items.stream().map(in -> {
            if (in.itemId() == null) {
                throw new IllegalArgumentException("Each line must reference an item");
            }
            catalogGateway.findActiveById(in.itemId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown or inactive item: " + in.itemId()));
            var price = pricingGateway.effectivePrice(clientId, in.itemId(), orderDate)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No price set for item %d for this client".formatted(in.itemId())));
            // For PER_DEPARTMENT clients each item must be assigned to a department (set in Atur
            // Harga) — that snapshot routes the line to the right department's monthly billing.
            Long departmentId = null;
            if (perDepartment) {
                departmentId = pricingGateway.departmentForItem(clientId, in.itemId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Item %d is not assigned to a department for this client".formatted(in.itemId())));
            }
            return new Order.NewLine(in.itemId(), in.quantity(), price, departmentId);
        }).toList();
    }

    private String generateOrderNumber(ClientSnapshot client, LocalDate date) {
        long seq = orderRepository.countByClientIdAndOrderDate(client.id(), date) + 1;
        return "%s-%s-%03d".formatted(client.clientCode(), date.format(ORDER_DATE_FORMAT), seq);
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

    // The only extensions allowed into the storage key. The fallback below is whitelisted against
    // this set so an untrusted client filename can never inject path separators / odd segments
    // (KI-10) — anything unrecognized defaults to .jpg.
    private static final Set<String> ALLOWED_PHOTO_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

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
        // Fall back to the client filename's suffix only when it is a known image extension —
        // never concatenate an unsanitized client string straight into the storage key (KI-10).
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0 && dot < filename.length() - 1) {
                var ext = filename.substring(dot).toLowerCase();
                if (ALLOWED_PHOTO_EXTENSIONS.contains(ext)) {
                    return ext;
                }
            }
        }
        return ".jpg";
    }
}