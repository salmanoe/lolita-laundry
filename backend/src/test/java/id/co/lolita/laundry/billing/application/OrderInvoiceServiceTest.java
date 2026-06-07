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
import id.co.lolita.laundry.shared.NotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void ensurePdfForOrder_returnsExisting_whenPdfAlreadyPresent() {
        var invoice = OrderInvoice.create("INV-AYI-20260601-001", 99L, 1L,
                LocalDate.of(2026, 6, 1), new BigDecimal("8000.00"));
        invoice.attachPdf("invoices/INV-AYI-20260601-001.pdf");
        when(invoiceRepository.findByOrderId(99L)).thenReturn(Optional.of(invoice));

        var result = service.ensurePdfForOrder(99L);

        assertThat(result.getPdfUrl()).isEqualTo("invoices/INV-AYI-20260601-001.pdf");
        verify(invoiceRepository, never()).save(any());
        verifyNoInteractions(deliveredOrders, clients, pdf, storage);
    }

    @Test
    void ensurePdfForOrder_rendersStoresAndSaves_whenPdfMissing() {
        var invoice = OrderInvoice.create("INV-AYI-20260601-001", 99L, 1L,
                LocalDate.of(2026, 6, 1), new BigDecimal("8000.00"));   // no PDF (e.g. backfilled)
        when(invoiceRepository.findByOrderId(99L)).thenReturn(Optional.of(invoice));
        var order = new DeliveredOrder(99L, "AYI-20260601-001", 1L, null, null,
                LocalDate.of(2026, 6, 1), BigDecimal.ONE, new BigDecimal("8000.00"),
                List.of(new InvoiceLine("Sheet King", "Pcs", new BigDecimal("2"),
                        new BigDecimal("4000"), new BigDecimal("8000.00"))));
        when(deliveredOrders.findDeliveredOrder(99L)).thenReturn(Optional.of(order));
        when(clients.findById(1L)).thenReturn(Optional.of(new ClientInfo(1L, "Are You and I", "AYI", false)));
        when(pdf.renderOrderInvoice(any())).thenReturn(new byte[]{1, 2, 3});
        when(storage.store(eq("invoices/INV-AYI-20260601-001.pdf"), any()))
                .thenReturn("invoices/INV-AYI-20260601-001.pdf");
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.ensurePdfForOrder(99L);

        assertThat(result.getPdfUrl()).isEqualTo("invoices/INV-AYI-20260601-001.pdf");
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void ensurePdfForOrder_throwsNotFound_whenNoInvoice() {
        when(invoiceRepository.findByOrderId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ensurePdfForOrder(99L))
                .isInstanceOf(NotFoundException.class);

        verify(invoiceRepository, never()).save(any());
        verifyNoInteractions(deliveredOrders, clients, pdf, storage);
    }

    @Test
    void regenerateAllPdfs_rerendersEveryInvoice() {
        var invoice = OrderInvoice.create("INV-AYI-20260601-001", 99L, 1L,
                LocalDate.of(2026, 6, 1), new BigDecimal("8000.00"));
        invoice.attachPdf("invoices/INV-AYI-20260601-001.pdf");   // already has a PDF — still re-rendered
        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        var order = new DeliveredOrder(99L, "AYI-20260601-001", 1L, null, null,
                LocalDate.of(2026, 6, 1), BigDecimal.ONE, new BigDecimal("8000.00"),
                List.of(new InvoiceLine("Sheet King", "Pcs", new BigDecimal("2"),
                        new BigDecimal("4000"), new BigDecimal("8000.00"))));
        when(deliveredOrders.findDeliveredOrder(99L)).thenReturn(Optional.of(order));
        when(clients.findById(1L)).thenReturn(Optional.of(new ClientInfo(1L, "Are You and I", "AYI", false)));
        when(pdf.renderOrderInvoice(any())).thenReturn(new byte[]{1, 2, 3});
        when(storage.store(eq("invoices/INV-AYI-20260601-001.pdf"), any()))
                .thenReturn("invoices/INV-AYI-20260601-001.pdf");
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.regenerateAllPdfs();

        assertThat(count).isEqualTo(1);
        verify(pdf).renderOrderInvoice(any());
        verify(invoiceRepository).save(invoice);
    }
}