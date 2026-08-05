package id.co.lolita.laundry.client.application;

import id.co.lolita.laundry.client.domain.BillingMode;
import id.co.lolita.laundry.client.domain.Client;
import id.co.lolita.laundry.client.domain.ClientType;
import id.co.lolita.laundry.client.domain.Department;
import id.co.lolita.laundry.client.domain.port.in.ClientDirectoryQuery;
import id.co.lolita.laundry.client.domain.port.in.ManageClientUseCase.CreateClientCommand;
import id.co.lolita.laundry.client.domain.port.in.ManageDepartmentUseCase.CreateDepartmentCommand;
import id.co.lolita.laundry.client.domain.port.in.ManagePriceListUseCase.SetPriceCommand;
import id.co.lolita.laundry.client.domain.port.out.BankAccountLookupGateway;
import id.co.lolita.laundry.client.domain.port.out.BankAccountLookupGateway.BankAccountRef;
import id.co.lolita.laundry.client.domain.port.out.ClientItemDepartmentRepository;
import id.co.lolita.laundry.client.domain.port.out.ClientPriceListRepository;
import id.co.lolita.laundry.client.domain.port.out.ClientRepository;
import id.co.lolita.laundry.client.domain.port.out.ClientTypeRepository;
import id.co.lolita.laundry.client.domain.port.out.DepartmentRepository;
import id.co.lolita.laundry.shared.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Orchestration rules of ClientService that aren't visible in the domain objects:
 * duplicate-code rejection, token generation, effective-date defaulting, inactive-token
 * rejection, and parent-existence checks. Pure Mockito — no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    ClientRepository clientRepository;
    @Mock
    DepartmentRepository departmentRepository;
    @Mock
    ClientPriceListRepository priceListRepository;
    @Mock
    ClientItemDepartmentRepository itemDepartmentRepository;
    @Mock
    ClientTypeRepository clientTypeRepository;
    @Mock
    BankAccountLookupGateway bankAccounts;
    @InjectMocks
    ClientService service;

    private static final long TYPE_ID = 1L;
    private static final long BANK_ACCOUNT_ID = 2L;

    private Client activeClient(long id) {
        return new Client(id, "X", "X", TYPE_ID, BillingMode.COMBINED,
                null, null, null, UUID.randomUUID(), null, true, null);
    }

    @Test
    void createClient_rejectsDuplicateCode() {
        when(clientRepository.existsByClientCode("PBS")).thenReturn(true);
        var cmd = new CreateClientCommand("Pasar Baru", "PBS", TYPE_ID,
                BillingMode.PER_DEPARTMENT, null, null, null, null);

        assertThatThrownBy(() -> service.createClient(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PBS");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClient_generatesTokenAndIsActive() {
        when(clientRepository.existsByClientCode("AYI")).thenReturn(false);
        when(clientTypeRepository.findById(TYPE_ID))
                .thenReturn(Optional.of(new ClientType(TYPE_ID, "HOTEL", "Hotel", 1, true)));
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var cmd = new CreateClientCommand("Are You and I", "AYI", TYPE_ID,
                BillingMode.COMBINED, "Reception", "022", "addr", null);

        var result = service.createClient(cmd);

        assertThat(result.getOrderToken()).isNotNull();
        assertThat(result.isActive()).isTrue();
        // No explicit bank account = bill to the default one; the lookup isn't even consulted.
        assertThat(result.getBankAccountId()).isNull();
        verifyNoInteractions(bankAccounts);
    }

    @Test
    void createClient_rejectsUnknownBankAccount() {
        when(clientRepository.existsByClientCode("PBS")).thenReturn(false);
        when(clientTypeRepository.findById(TYPE_ID))
                .thenReturn(Optional.of(new ClientType(TYPE_ID, "HOTEL", "Hotel", 1, true)));
        when(bankAccounts.findById(BANK_ACCOUNT_ID)).thenReturn(Optional.empty());
        var cmd = new CreateClientCommand("Pasar Baru", "PBS", TYPE_ID,
                BillingMode.PER_DEPARTMENT, null, null, null, BANK_ACCOUNT_ID);

        assertThatThrownBy(() -> service.createClient(cmd))
                .isInstanceOf(NotFoundException.class);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClient_rejectsInactiveBankAccount() {
        when(clientRepository.existsByClientCode("PBS")).thenReturn(false);
        when(clientTypeRepository.findById(TYPE_ID))
                .thenReturn(Optional.of(new ClientType(TYPE_ID, "HOTEL", "Hotel", 1, true)));
        when(bankAccounts.findById(BANK_ACCOUNT_ID))
                .thenReturn(Optional.of(new BankAccountRef(BANK_ACCOUNT_ID, "Rekening Lama", false)));
        var cmd = new CreateClientCommand("Pasar Baru", "PBS", TYPE_ID,
                BillingMode.PER_DEPARTMENT, null, null, null, BANK_ACCOUNT_ID);

        assertThatThrownBy(() -> service.createClient(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rekening Lama");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClient_acceptsActiveBankAccount() {
        when(clientRepository.existsByClientCode("PBS")).thenReturn(false);
        when(clientTypeRepository.findById(TYPE_ID))
                .thenReturn(Optional.of(new ClientType(TYPE_ID, "HOTEL", "Hotel", 1, true)));
        when(bankAccounts.findById(BANK_ACCOUNT_ID))
                .thenReturn(Optional.of(new BankAccountRef(BANK_ACCOUNT_ID, "Rekening Perusahaan", true)));
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var cmd = new CreateClientCommand("Pasar Baru", "PBS", TYPE_ID,
                BillingMode.PER_DEPARTMENT, null, null, null, BANK_ACCOUNT_ID);

        assertThat(service.createClient(cmd).getBankAccountId()).isEqualTo(BANK_ACCOUNT_ID);
    }

    @Test
    void setPrice_defaultsEffectiveDateToToday_whenNull() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(activeClient(1L)));
        when(priceListRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.setPrice(new SetPriceCommand(1L, 10L, new BigDecimal("5000"), null, null));

        assertThat(saved.effectiveDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void setPrice_rejectsUnknownClient() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setPrice(new SetPriceCommand(99L, 10L, BigDecimal.TEN, null, null)))
                .isInstanceOf(NotFoundException.class);
        verify(priceListRepository, never()).save(any());
    }

    @Test
    void createDepartment_rejectsUnknownClient() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createDepartment(new CreateDepartmentCommand(99L, "Room Linen")))
                .isInstanceOf(NotFoundException.class);
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void allDepartments_includesInactive_whileActiveDepartmentsFiltersThem() {
        // KI-5: historical labels (billing/invoice) resolve a deactivated department's name; the
        // order-form path stays active-only. Departments are soft-deactivated, never deleted.
        var roomLinen = new Department(10L, 1L, "Room Linen", true);
        var oldFnb = new Department(20L, 1L, "F&B Linen", false);   // deactivated
        when(departmentRepository.findByClientId(1L)).thenReturn(List.of(roomLinen, oldFnb));

        assertThat(service.allDepartments(1L)).extracting(ClientDirectoryQuery.DepartmentView::name)
                .containsExactly("Room Linen", "F&B Linen");
        assertThat(service.activeDepartments(1L)).extracting(ClientDirectoryQuery.DepartmentView::name)
                .containsExactly("Room Linen");
    }

}
