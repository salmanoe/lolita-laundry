package id.co.lolita.laundry.billing.application;

import id.co.lolita.laundry.billing.domain.BillingStatus;
import id.co.lolita.laundry.billing.domain.MonthlyBilling;
import id.co.lolita.laundry.billing.domain.port.in.GenerateMonthlyBillingUseCase.GenerateCommand;
import id.co.lolita.laundry.billing.domain.port.in.UpdateBillingStatusUseCase.UpdateStatusCommand;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway;
import id.co.lolita.laundry.billing.domain.port.out.BillingClientGateway.ClientInfo;
import id.co.lolita.laundry.billing.domain.port.out.BillingStoragePort;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway;
import id.co.lolita.laundry.billing.domain.port.out.DeliveredOrderGateway.DeliveredOrder;
import id.co.lolita.laundry.billing.domain.port.out.InvoicePdfPort;
import id.co.lolita.laundry.billing.domain.port.out.MonthlyBillingRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    InvoicePdfPort pdf;
    @Mock
    BillingStoragePort storage;
    @InjectMocks
    MonthlyBillingService service;

    private static final long COMBINED_CLIENT = 1L;
    private static final long PBS = 7L;

    private static ClientInfo combined() {
        return new ClientInfo(COMBINED_CLIENT, "Are You and I", "AYI", false);
    }

    private static ClientInfo perDepartment() {
        return new ClientInfo(PBS, "Pasar Baru Square", "PBS", true);
    }

    private static DeliveredOrder order(String number, Long deptId, String deptName, String total) {
        return new DeliveredOrder(System.nanoTime(), number, PBS, deptId, deptName,
                LocalDate.of(2026, 6, 1), BigDecimal.ONE, new BigDecimal(total), List.of());
    }

    private void stubPdfAndStorageAndSave() {
        when(pdf.renderMonthlyBilling(any())).thenReturn(new byte[]{1, 2, 3});
        when(storage.store(any(), any())).thenReturn("billings/key.pdf");
        when(billingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void generate_combinedClient_producesOneBilling() {
        when(clients.findById(COMBINED_CLIENT)).thenReturn(Optional.of(combined()));
        when(deliveredOrders.findDeliveredOrders(COMBINED_CLIENT, 2026, 6)).thenReturn(List.of(
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
        when(deliveredOrders.findDeliveredOrders(PBS, 2026, 6)).thenReturn(List.of(
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
        when(deliveredOrders.findDeliveredOrders(COMBINED_CLIENT, 2026, 6))
                .thenReturn(List.of(order("AYI-20260601-001", null, null, "5000.00")));
        var existingDraft = new MonthlyBilling(50L, "BILL-AYI-202606", COMBINED_CLIENT, null, 2026, 6,
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
        when(deliveredOrders.findDeliveredOrders(COMBINED_CLIENT, 2026, 6))
                .thenReturn(List.of(order("AYI-20260601-001", null, null, "5000.00")));
        var issued = new MonthlyBilling(50L, "BILL-AYI-202606", COMBINED_CLIENT, null, 2026, 6,
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
        when(deliveredOrders.findDeliveredOrders(COMBINED_CLIENT, 2026, 6)).thenReturn(List.of());

        assertThatThrownBy(() -> service.generate(new GenerateCommand(COMBINED_CLIENT, 2026, 6)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No delivered orders");
        verify(billingRepository, never()).save(any());
    }

    @Test
    void generate_rejectsUnknownClient() {
        when(clients.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(new GenerateCommand(99L, 2026, 6)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateStatus_advancesOneStep() {
        var draft = new MonthlyBilling(50L, "BILL-AYI-202606", COMBINED_CLIENT, null, 2026, 6,
                LocalDate.now(), new BigDecimal("100.00"), BillingStatus.DRAFT, null, null, Instant.now(), List.of());
        when(billingRepository.findById(50L)).thenReturn(Optional.of(draft));
        when(billingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateStatus(new UpdateStatusCommand(50L, BillingStatus.ISSUED));

        assertThat(result.getStatus()).isEqualTo(BillingStatus.ISSUED);
    }
}