package id.co.lolita.laundry.billing.application;

import id.co.lolita.laundry.billing.domain.OrderInvoice;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway.ClientInfo;
import id.co.lolita.laundry.billing.domain.port.out.BillingStoragePort;
import id.co.lolita.laundry.billing.domain.port.out.CompanyProfileGateway;
import id.co.lolita.laundry.billing.domain.port.out.CompanyProfileGateway.CompanyInfo;
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
 * OrderInvoiceService: the create-or-refresh-and-attach-PDF flow. The invoice is viewable from
 * RECEIVED (live preview, re-rendered each view) and frozen once the order is DELIVERED.
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
    CompanyProfileGateway companyProfile;
    @Mock
    InvoicePdfPort pdf;
    @Mock
    BillingStoragePort storage;
    @InjectMocks
    OrderInvoiceService service;

    private static final CompanyInfo COMPANY = new CompanyInfo("Lolita Laundry",
            "Jl. Sukaraja No. 318 Bandung", "082318359775", "Alban Valentino Ramatir",
            "Bank BCA", "4061792362", "Lolita Laundry");

    private static DeliveredOrder order(boolean delivered) {
        return new DeliveredOrder(99L, "AYI-20260601-001", 1L,
                LocalDate.of(2026, 6, 1), BigDecimal.ONE, new BigDecimal("8000.00"), delivered,
                List.of(new InvoiceLine("Sheet King", "Pcs", new BigDecimal("2"),
                        new BigDecimal("4000"), new BigDecimal("8000.00"), null, null)));
    }

    @Test
    void createForDeliveredOrder_buildsInvoice_rendersPdf_andStores() {
        when(invoiceRepository.findByOrderId(99L)).thenReturn(Optional.empty());
        when(deliveredOrders.findBillableOrder(99L)).thenReturn(Optional.of(order(true)));
        when(clients.findById(1L)).thenReturn(Optional.of(new ClientInfo(1L, "Are You and I", "AYI", false)));
        when(companyProfile.current()).thenReturn(COMPANY);
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
        // Company letterhead snapshotted onto the invoice.
        assertThat(saved.getCompanyName()).isEqualTo("Lolita Laundry");
        assertThat(saved.getCompanyAddress()).isEqualTo("Jl. Sukaraja No. 318 Bandung");
        assertThat(saved.getCompanyPhone()).isEqualTo("082318359775");
    }

    @Test
    void createForDeliveredOrder_refreshesExistingPreview_atDelivery() {
        // A preview was rendered while the order was open; delivery re-renders the same row.
        var preview = OrderInvoice.create("INV-AYI-20260601-001", 99L, 1L,
                LocalDate.of(2026, 6, 1), new BigDecimal("4000.00"),
                "Lolita Laundry", "Jl. Sukaraja No. 318 Bandung", "082318359775");
        preview.attachPdf("invoices/INV-AYI-20260601-001.pdf");
        when(invoiceRepository.findByOrderId(99L)).thenReturn(Optional.of(preview));
        when(deliveredOrders.findBillableOrder(99L)).thenReturn(Optional.of(order(true)));
        when(clients.findById(1L)).thenReturn(Optional.of(new ClientInfo(1L, "Are You and I", "AYI", false)));
        when(companyProfile.current()).thenReturn(COMPANY);
        when(pdf.renderOrderInvoice(any())).thenReturn(new byte[]{1, 2, 3});
        when(storage.store(eq("invoices/INV-AYI-20260601-001.pdf"), any())).thenReturn("invoices/INV-AYI-20260601-001.pdf");
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createForDeliveredOrder(99L);

        // The same row is refreshed to the final order total.
        verify(invoiceRepository).save(preview);
        assertThat(preview.getSubtotal()).isEqualByComparingTo("8000.00");
    }

    @Test
    void prepareInvoiceForOrder_returnsExisting_whenDeliveredAndPdfPresent() {
        var invoice = OrderInvoice.create("INV-AYI-20260601-001", 99L, 1L,
                LocalDate.of(2026, 6, 1), new BigDecimal("8000.00"),
                "Lolita Laundry", "Jl. Sukaraja No. 318 Bandung", "082318359775");
        invoice.attachPdf("invoices/INV-AYI-20260601-001.pdf");
        when(invoiceRepository.findByOrderId(99L)).thenReturn(Optional.of(invoice));
        when(deliveredOrders.findBillableOrder(99L)).thenReturn(Optional.of(order(true)));

        var result = service.prepareInvoiceForOrder(99L);

        assertThat(result.getPdfUrl()).isEqualTo("invoices/INV-AYI-20260601-001.pdf");
        // Frozen: no re-render once delivered.
        verify(invoiceRepository, never()).save(any());
        verifyNoInteractions(clients, pdf, storage, companyProfile);
    }

    @Test
    void prepareInvoiceForOrder_refreshesPreview_whenOrderStillOpen() {
        // Existing preview with a PDF, but the order is not yet delivered → re-render.
        var preview = OrderInvoice.create("INV-AYI-20260601-001", 99L, 1L,
                LocalDate.of(2026, 6, 1), new BigDecimal("4000.00"),
                "Lolita Laundry", "Jl. Sukaraja No. 318 Bandung", "082318359775");
        preview.attachPdf("invoices/INV-AYI-20260601-001.pdf");
        when(invoiceRepository.findByOrderId(99L)).thenReturn(Optional.of(preview));
        when(deliveredOrders.findBillableOrder(99L)).thenReturn(Optional.of(order(false)));
        when(clients.findById(1L)).thenReturn(Optional.of(new ClientInfo(1L, "Are You and I", "AYI", false)));
        when(companyProfile.current()).thenReturn(COMPANY);
        when(pdf.renderOrderInvoice(any())).thenReturn(new byte[]{1, 2, 3});
        when(storage.store(eq("invoices/INV-AYI-20260601-001.pdf"), any())).thenReturn("invoices/INV-AYI-20260601-001.pdf");
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.prepareInvoiceForOrder(99L);

        verify(invoiceRepository).save(preview);
        assertThat(preview.getSubtotal()).isEqualByComparingTo("8000.00");
    }

    @Test
    void prepareInvoiceForOrder_createsPreview_whenNoInvoiceYet() {
        when(invoiceRepository.findByOrderId(99L)).thenReturn(Optional.empty());
        when(deliveredOrders.findBillableOrder(99L)).thenReturn(Optional.of(order(false)));
        when(clients.findById(1L)).thenReturn(Optional.of(new ClientInfo(1L, "Are You and I", "AYI", false)));
        when(companyProfile.current()).thenReturn(COMPANY);
        when(pdf.renderOrderInvoice(any())).thenReturn(new byte[]{1, 2, 3});
        when(storage.store(eq("invoices/INV-AYI-20260601-001.pdf"), any())).thenReturn("invoices/INV-AYI-20260601-001.pdf");
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.prepareInvoiceForOrder(99L);

        assertThat(result.getInvoiceNumber()).isEqualTo("INV-AYI-20260601-001");
        assertThat(result.getPdfUrl()).isEqualTo("invoices/INV-AYI-20260601-001.pdf");
        verify(invoiceRepository).save(any());
    }

    @Test
    void prepareInvoiceForOrder_throwsNotFound_whenOrderNotBillable() {
        when(invoiceRepository.findByOrderId(99L)).thenReturn(Optional.empty());
        when(deliveredOrders.findBillableOrder(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.prepareInvoiceForOrder(99L))
                .isInstanceOf(NotFoundException.class);

        verify(invoiceRepository, never()).save(any());
        verifyNoInteractions(clients, pdf, storage);
    }

    @Test
    void regenerateAllPdfs_rerendersEveryInvoice() {
        var invoice = OrderInvoice.create("INV-AYI-20260601-001", 99L, 1L,
                LocalDate.of(2026, 6, 1), new BigDecimal("8000.00"),
                "Lolita Laundry", "Jl. Sukaraja No. 318 Bandung", "082318359775");
        invoice.attachPdf("invoices/INV-AYI-20260601-001.pdf");   // already has a PDF — still re-rendered
        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        when(deliveredOrders.findBillableOrder(99L)).thenReturn(Optional.of(order(true)));
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
