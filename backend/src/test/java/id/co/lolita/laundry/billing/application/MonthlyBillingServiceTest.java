package id.co.lolita.laundry.billing.application;

import id.co.lolita.laundry.billing.domain.BillingStatus;
import id.co.lolita.laundry.billing.domain.MonthlyBilling;
import id.co.lolita.laundry.billing.domain.MonthlyBillingLine;
import id.co.lolita.laundry.billing.domain.port.in.GenerateMonthlyBillingUseCase.GenerateCommand;
import id.co.lolita.laundry.billing.domain.port.in.UpdateBillingStatusUseCase.UpdateStatusCommand;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway.ClientInfo;
import id.co.lolita.laundry.billing.domain.port.out.BillingStoragePort;
import id.co.lolita.laundry.billing.domain.port.out.CompanyProfileGateway;
import id.co.lolita.laundry.billing.domain.port.out.CompanyProfileGateway.CompanyInfo;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway.DeliveredOrder;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort.MonthlyBillingDocument;
import id.co.lolita.laundry.billing.domain.port.out.MonthlyBillingRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
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
 * Orchestration rules of MonthlyBillingService: COMBINED vs PER_DEPARTMENT grouping, the
 * regeneration policy (replace DRAFT / reject ISSUED), the empty-period guard, and status
 * advancement. Pure Mockito — PDF rendering and storage are stubbed.
 */
@ExtendWith(MockitoExtension.class)
class MonthlyBillingServiceTest {

    @Mock
    MonthlyBillingRepository billingRepository;
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
    MonthlyBillingService service;

    private static final long COMBINED_CLIENT = 1L;
    private static final long PBS = 7L;

    private static final CompanyInfo COMPANY = new CompanyInfo("Lolita Laundry",
            "Jl. Sukaraja No. 318 Bandung", "082318359775", "Alban Valentino Ramatir",
            "Bank BCA", "4061792362", "Lolita Laundry");

    private static ClientInfo combined() {
        return new ClientInfo(COMBINED_CLIENT, "Are You and I", "AYI", false);
    }

    private static ClientInfo perDepartment() {
        return new ClientInfo(PBS, "Pasar Baru Square", "PBS", true);
    }

    private static DeliveredOrder order(String number, Long deptId, String deptName, String total) {
        var line = new DeliveredOrderGateway.InvoiceLine("Item", "Pcs", BigDecimal.ONE,
                new BigDecimal(total), new BigDecimal(total), deptId, deptName);
        return new DeliveredOrder(System.nanoTime(), number, PBS,
                LocalDate.of(2026, 6, 1), BigDecimal.ONE, new BigDecimal(total), List.of(line));
    }

    private void stubPdfAndStorageAndSave() {
        // DRAFT billings render from the live company profile.
        when(companyProfile.current()).thenReturn(COMPANY);
        when(pdf.renderMonthlyBilling(any())).thenReturn(new byte[]{1, 2, 3});
        when(storage.store(any(), any())).thenReturn("billings/key.pdf");
        when(billingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void generate_combinedClient_producesOneBilling() {
        when(clients.findById(COMBINED_CLIENT)).thenReturn(Optional.of(combined()));
        when(deliveredOrders.findBillableOrders(COMBINED_CLIENT, 2026, 6)).thenReturn(List.of(
                order("AYI-20260601-001", null, null, "5000.00"),
                order("AYI-20260602-001", null, null, "3000.00")));
        when(billingRepository.findExisting(eq(COMBINED_CLIENT), eq(null), eq(2026), eq(6)))
                .thenReturn(Optional.empty());
        stubPdfAndStorageAndSave();

        List<MonthlyBilling> result = service.generate(new GenerateCommand(COMBINED_CLIENT, 2026, 6));

        assertThat(result).singleElement().satisfies(b -> {
            assertThat(b.getDepartmentId()).isNull();
            assertThat(b.getTotal()).isEqualByComparingTo("8000.00");
            assertThat(b.getBillingNumber()).isEqualTo("BILL-AYI-202606");
            assertThat(b.getPdfUrl()).isEqualTo("billings/key.pdf");
        });
    }

    @Test
    void generate_perDepartmentClient_splitsByDepartment() {
        when(clients.findById(PBS)).thenReturn(Optional.of(perDepartment()));
        when(deliveredOrders.findBillableOrders(PBS, 2026, 6)).thenReturn(List.of(
                order("PBS-20260601-001", 10L, "Room Linen", "5000.00"),
                order("PBS-20260602-001", 10L, "Room Linen", "5000.00"),
                order("PBS-20260603-001", 20L, "F&B Linen", "3000.00")));
        when(billingRepository.findExisting(eq(PBS), any(), eq(2026), eq(6))).thenReturn(Optional.empty());
        stubPdfAndStorageAndSave();

        List<MonthlyBilling> result = service.generate(new GenerateCommand(PBS, 2026, 6));

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(b -> {
            assertThat(b.getDepartmentId()).isEqualTo(10L);
            assertThat(b.getTotal()).isEqualByComparingTo("10000.00");
            assertThat(b.getBillingNumber()).isEqualTo("BILL-PBS-202606-RL");
        });
        assertThat(result).anySatisfy(b -> {
            assertThat(b.getDepartmentId()).isEqualTo(20L);
            assertThat(b.getTotal()).isEqualByComparingTo("3000.00");
            assertThat(b.getBillingNumber()).isEqualTo("BILL-PBS-202606-FBL");
        });
    }

    @Test
    void generate_replacesExistingDraft() {
        when(clients.findById(COMBINED_CLIENT)).thenReturn(Optional.of(combined()));
        when(deliveredOrders.findBillableOrders(COMBINED_CLIENT, 2026, 6))
                .thenReturn(List.of(order("AYI-20260601-001", null, null, "5000.00")));
        var existingDraft = new MonthlyBilling(50L, "BILL-AYI-202606", COMBINED_CLIENT, null, null, 2026, 6,
                LocalDate.now(), new BigDecimal("100.00"), BillingStatus.DRAFT, null, null, Instant.now(), List.of());
        when(billingRepository.findExisting(eq(COMBINED_CLIENT), eq(null), eq(2026), eq(6)))
                .thenReturn(Optional.of(existingDraft));
        stubPdfAndStorageAndSave();

        service.generate(new GenerateCommand(COMBINED_CLIENT, 2026, 6));

        verify(billingRepository).deleteById(50L);
        verify(billingRepository).save(any());
    }

    @Test
    void generate_rejectsRegenerationOfIssuedBilling() {
        when(clients.findById(COMBINED_CLIENT)).thenReturn(Optional.of(combined()));
        when(deliveredOrders.findBillableOrders(COMBINED_CLIENT, 2026, 6))
                .thenReturn(List.of(order("AYI-20260601-001", null, null, "5000.00")));
        var issued = new MonthlyBilling(50L, "BILL-AYI-202606", COMBINED_CLIENT, null, null, 2026, 6,
                LocalDate.now(), new BigDecimal("100.00"), BillingStatus.ISSUED, null, null, Instant.now(), List.of());
        when(billingRepository.findExisting(eq(COMBINED_CLIENT), eq(null), eq(2026), eq(6)))
                .thenReturn(Optional.of(issued));

        assertThatThrownBy(() -> service.generate(new GenerateCommand(COMBINED_CLIENT, 2026, 6)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be regenerated");
        verify(billingRepository, never()).deleteById(any());
        verify(billingRepository, never()).save(any());
    }

    @Test
    void generate_rejectsEmptyPeriod() {
        when(clients.findById(COMBINED_CLIENT)).thenReturn(Optional.of(combined()));
        when(deliveredOrders.findBillableOrders(COMBINED_CLIENT, 2026, 6)).thenReturn(List.of());

        assertThatThrownBy(() -> service.generate(new GenerateCommand(COMBINED_CLIENT, 2026, 6)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tidak ada order");
        verify(billingRepository, never()).save(any());
    }

    @Test
    void generate_rejectsUnknownClient() {
        when(clients.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(new GenerateCommand(99L, 2026, 6)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateStatus_issuing_freezesCompanySnapshot_andRerenders() {
        var draft = new MonthlyBilling(50L, "BILL-AYI-202606", COMBINED_CLIENT, null, null, 2026, 6,
                LocalDate.now(), new BigDecimal("100.00"), BillingStatus.DRAFT, null, null, Instant.now(), List.of());
        when(billingRepository.findById(50L)).thenReturn(Optional.of(draft));
        when(clients.findById(COMBINED_CLIENT)).thenReturn(Optional.of(combined()));
        when(companyProfile.current()).thenReturn(COMPANY);
        when(pdf.renderMonthlyBilling(any())).thenReturn(new byte[]{1, 2, 3});
        when(storage.store(eq("billings/BILL-AYI-202606.pdf"), any())).thenReturn("billings/BILL-AYI-202606.pdf");
        when(billingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateStatus(new UpdateStatusCommand(50L, BillingStatus.ISSUED));

        assertThat(result.getStatus()).isEqualTo(BillingStatus.ISSUED);
        // The letterhead + bank block are frozen onto the billing at issue time.
        assertThat(result.getCompanyName()).isEqualTo("Lolita Laundry");
        assertThat(result.getBankAccount()).isEqualTo("4061792362");
        assertThat(result.getPdfUrl()).isEqualTo("billings/BILL-AYI-202606.pdf");
        verify(pdf).renderMonthlyBilling(any());
    }

    @Test
    void updateStatus_paying_doesNotRecaptureCompany() {
        // An ISSUED→PAID transition keeps the snapshot frozen at issue and does not re-render.
        var issued = new MonthlyBilling(50L, "BILL-AYI-202606", COMBINED_CLIENT, null, null, 2026, 6,
                LocalDate.now(), new BigDecimal("100.00"), BillingStatus.ISSUED, "billings/k.pdf", null,
                Instant.now(), List.of());
        when(billingRepository.findById(50L)).thenReturn(Optional.of(issued));
        when(billingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateStatus(new UpdateStatusCommand(50L, BillingStatus.PAID));

        assertThat(result.getStatus()).isEqualTo(BillingStatus.PAID);
        verify(pdf, never()).renderMonthlyBilling(any());
        verifyNoInteractions(companyProfile);
    }

    @Test
    void regenerateAllPdfs_rerendersIssuedFromFrozenSnapshot_notLiveProfile() {
        // A PAID billing with an old PDF and a frozen company snapshot. Bulk refresh re-renders it
        // (layout-only) from that snapshot — never the live profile, so history is not rewritten.
        var paid = new MonthlyBilling(50L, "BILL-AYI-202606", COMBINED_CLIENT, null, null, 2026, 6,
                LocalDate.now(), new BigDecimal("100.00"), BillingStatus.PAID, "billings/old.pdf", null,
                Instant.now(), List.of());
        paid.captureCompany("Lolita Laundry", "OLD ADDRESS", "0000", "Old Beneficiary",
                "Bank BCA", "9999999999", "Lolita Laundry");
        when(billingRepository.findAll(null, null, null)).thenReturn(List.of(paid));
        when(clients.findById(COMBINED_CLIENT)).thenReturn(Optional.of(combined()));
        when(pdf.renderMonthlyBilling(any())).thenReturn(new byte[]{1, 2, 3});
        when(storage.store(eq("billings/BILL-AYI-202606.pdf"), any())).thenReturn("billings/BILL-AYI-202606.pdf");
        when(billingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.regenerateAllPdfs();

        assertThat(count).isEqualTo(1);
        var doc = ArgumentCaptor.forClass(MonthlyBillingDocument.class);
        verify(pdf).renderMonthlyBilling(doc.capture());
        assertThat(doc.getValue().company().address()).isEqualTo("OLD ADDRESS");
        assertThat(doc.getValue().company().bankAccount()).isEqualTo("9999999999");
        verify(billingRepository).save(paid);
        verifyNoInteractions(companyProfile);   // frozen — live profile never consulted
    }

    @Test
    void sync_addsBillableOrderToNewDraftPeriod() {
        var line = new DeliveredOrderGateway.InvoiceLine("Item", "Pcs", BigDecimal.ONE,
                new BigDecimal("5000.00"), new BigDecimal("5000.00"), null, null);
        var o = new DeliveredOrder(77L, "AYI-20260601-001", COMBINED_CLIENT,
                LocalDate.of(2026, 6, 1), BigDecimal.ONE, new BigDecimal("5000.00"), List.of(line));
        when(deliveredOrders.findBillableOrder(77L)).thenReturn(Optional.of(o));
        when(billingRepository.findAllByOrderLine(77L)).thenReturn(List.of());
        when(clients.findById(COMBINED_CLIENT)).thenReturn(Optional.of(combined()));
        when(billingRepository.findExisting(COMBINED_CLIENT, null, 2026, 6)).thenReturn(Optional.empty());
        stubPdfAndStorageAndSave();

        service.sync(77L);

        var captor = ArgumentCaptor.forClass(MonthlyBilling.class);
        verify(billingRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getBillingNumber()).isEqualTo("BILL-AYI-202606");
        assertThat(saved.getTotal()).isEqualByComparingTo("5000.00");
        assertThat(saved.getLines()).singleElement().satisfies(l -> assertThat(l.orderId()).isEqualTo(77L));
    }

    @Test
    void sync_editOnIssuedBill_rollsDeltaIntoNextOpenDraft() {
        // KI-3 (option b): an order is on an ISSUED June bill at 5000; staff edit it to 8000. The
        // frozen bill is untouched and the +3000 delta is rolled into the next open DRAFT (July).
        var line = new DeliveredOrderGateway.InvoiceLine("Item", "Pcs", BigDecimal.ONE,
                new BigDecimal("8000.00"), new BigDecimal("8000.00"), null, null);
        var edited = new DeliveredOrder(77L, "AYI-20260601-001", COMBINED_CLIENT,
                LocalDate.of(2026, 6, 1), BigDecimal.ONE, new BigDecimal("8000.00"), List.of(line));
        var issuedJune = new MonthlyBilling(50L, "BILL-AYI-202606", COMBINED_CLIENT, null, null, 2026, 6,
                LocalDate.now(), new BigDecimal("5000.00"), BillingStatus.ISSUED, "billings/k.pdf", null, Instant.now(),
                List.of(MonthlyBillingLine.of(77L, "AYI-20260601-001", LocalDate.of(2026, 6, 1), new BigDecimal("5000.00"))));
        when(deliveredOrders.findBillableOrder(77L)).thenReturn(Optional.of(edited));
        when(billingRepository.findAllByOrderLine(77L)).thenReturn(List.of(issuedJune));
        when(clients.findById(COMBINED_CLIENT)).thenReturn(Optional.of(combined()));
        when(billingRepository.findExisting(COMBINED_CLIENT, null, 2026, 6)).thenReturn(Optional.of(issuedJune));
        when(billingRepository.findExisting(COMBINED_CLIENT, null, 2026, 7)).thenReturn(Optional.empty());
        stubPdfAndStorageAndSave();

        service.sync(77L);

        var captor = ArgumentCaptor.forClass(MonthlyBilling.class);
        verify(billingRepository).save(captor.capture());   // only the new July draft is written
        var saved = captor.getValue();
        assertThat(saved.getBillingNumber()).isEqualTo("BILL-AYI-202607");
        assertThat(saved.getStatus()).isEqualTo(BillingStatus.DRAFT);
        assertThat(saved.getTotal()).isEqualByComparingTo("3000.00");   // the rolled-forward delta
        assertThat(saved.getLines()).singleElement().satisfies(l -> {
            assertThat(l.orderId()).isEqualTo(77L);
            assertThat(l.subtotal()).isEqualByComparingTo("3000.00");
        });
        verify(billingRepository, never()).deleteById(any());
    }

    @Test
    void sync_unchangedOrderOnIssuedBill_isNoOp() {
        // The order is on an ISSUED bill and its amount has not changed → zero delta, nothing rolls
        // forward, the frozen bill stays frozen. No writes at all.
        var line = new DeliveredOrderGateway.InvoiceLine("Item", "Pcs", BigDecimal.ONE,
                new BigDecimal("5000.00"), new BigDecimal("5000.00"), null, null);
        var unchanged = new DeliveredOrder(77L, "AYI-20260601-001", COMBINED_CLIENT,
                LocalDate.of(2026, 6, 1), BigDecimal.ONE, new BigDecimal("5000.00"), List.of(line));
        var issuedJune = new MonthlyBilling(50L, "BILL-AYI-202606", COMBINED_CLIENT, null, null, 2026, 6,
                LocalDate.now(), new BigDecimal("5000.00"), BillingStatus.ISSUED, "billings/k.pdf", null, Instant.now(),
                List.of(MonthlyBillingLine.of(77L, "AYI-20260601-001", LocalDate.of(2026, 6, 1), new BigDecimal("5000.00"))));
        when(deliveredOrders.findBillableOrder(77L)).thenReturn(Optional.of(unchanged));
        when(billingRepository.findAllByOrderLine(77L)).thenReturn(List.of(issuedJune));
        when(clients.findById(COMBINED_CLIENT)).thenReturn(Optional.of(combined()));

        service.sync(77L);

        verify(billingRepository, never()).save(any());
        verify(billingRepository, never()).deleteById(any());
        verifyNoInteractions(pdf, storage);
    }

    @Test
    void sync_removesCancelledOrderAndDeletesEmptyDraft() {
        when(deliveredOrders.findBillableOrder(77L)).thenReturn(Optional.empty());   // cancelled / gone
        var draft = new MonthlyBilling(50L, "BILL-AYI-202606", COMBINED_CLIENT, null, null, 2026, 6,
                LocalDate.now(), new BigDecimal("5000.00"), BillingStatus.DRAFT, "billings/k.pdf", null, Instant.now(),
                List.of(MonthlyBillingLine.of(77L, "AYI-20260601-001", LocalDate.of(2026, 6, 1), new BigDecimal("5000.00"))));
        when(billingRepository.findAllByOrderLine(77L)).thenReturn(List.of(draft));

        service.sync(77L);

        verify(billingRepository).deleteById(50L);   // last line removed → billing deleted
        verify(billingRepository, never()).save(any());
    }
}