package id.co.lolita.laundry.billing.domain.port.out;

import id.co.lolita.laundry.billing.domain.OrderInvoice;

import java.util.List;
import java.util.Optional;

public interface OrderInvoiceRepository {

    OrderInvoice save(OrderInvoice invoice);

    Optional<OrderInvoice> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    /** Every invoice — used by the bulk "regenerate all PDFs" admin action. */
    List<OrderInvoice> findAll();
}