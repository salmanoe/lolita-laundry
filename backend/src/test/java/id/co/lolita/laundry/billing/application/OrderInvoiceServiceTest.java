package id.co.lolita.laundry.billing.application;

import id.co.lolita.laundry.billing.domain.OrderInvoice;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway.ClientInfo;
import id.co.lolita.laundry.billing.domain.port.out.BillingStoragePort;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway.DeliveredOrder;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway.InvoiceLine;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort;
import id.co.lolita.laundry.billing.domain.port.out.OrderInvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * OrderInvoiceService: idempotency (event redelivery is safe) and the create-and-attach-PDF flow.
 */
@ExtendWith(MockitoExtension.class)
class OrderInvoiceServiceTest {

    @Mock
    OrderInvoiceRepository invoiceRepository;
    @Mock
    DeliveredOrderGateway deliveredOrders;
    @Mock
    BillingClientGateway clients;
    @Mock
    InvoicePdfPort pdf;
    @Mock
    BillingStoragePort storage;
    @InjectMocks
    OrderInvoiceService service;

    @Test
    void createForDeliveredOrder_isIdempotent_whenInvoiceExists() {
        when(invoiceRepository.existsByOrderId(99L)).thenReturn(true);

        service.createForDeliveredOrder(99L);

        verify(invoiceRepository, never()).save(any());
        verifyNoInteractions(deliveredOrders, clients, pdf, storage);
    }

    @Test
    void createForDeliveredOrder_buildsInvoice_rendersPdf_andStores() {
        when(invoiceRepository.existsByOrderId(99L)).thenReturn(false);
        var order = new DeliveredOrder(99L, "AYI-20260601-001", 1L, null, null,
                LocalDate.of(2026, 6, 1), BigDecimal.ONE, new BigDecimal("8000.00"),
                List.of(new InvoiceLine("Sheet King", "Pcs", new BigDecimal("2"),
                        new BigDecimal("4000"), new BigDecimal("8000.00"))));
        when(deliveredOrders.findDeliveredOrder(99L)).thenReturn(Optional.of(order));
        when(clients.findById(1L)).thenReturn(Optional.of(new ClientInfo(1L, "Are You and I", "AYI", false)));
        when(pdf.renderOrderInvoice(any())).thenReturn(new byte[]{1, 2, 3});
        when(storage.store(eq("invoices/INV-AYI-20260601-001.pdf"), any())).thenReturn("invoices/INV-AYI-20260601-001.pdf");
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createForDeliveredOrder(99L);

        ArgumentCaptor<OrderInvoice> captor = ArgumentCaptor.forClass(OrderInvoice.class);
        verify(invoiceRepository).save(captor.capture());
        OrderInvoice saved = captor.getValue();
        assertThat(saved.getInvoiceNumber()).isEqualTo("INV-AYI-20260601-001");
        assertThat(saved.getOrderId()).isEqualTo(99L);
        assertThat(saved.getSubtotal()).isEqualByComparingTo("8000.00");
        assertThat(saved.getPdfUrl()).isEqualTo("invoices/INV-AYI-20260601-001.pdf");
    }
}