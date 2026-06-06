package id.co.lolita.laundry.billing.domain.port.out;

import id.co.lolita.laundry.billing.domain.OrderInvoice;

import java.util.Optional;

public interface OrderInvoiceRepository {

    OrderInvoice save(OrderInvoice invoice);

    Optional<OrderInvoice> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);
}