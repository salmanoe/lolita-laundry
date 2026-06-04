package id.co.lolita.laundry.client.application;

import id.co.lolita.laundry.client.domain.BillingMode;
import id.co.lolita.laundry.client.domain.Client;
import id.co.lolita.laundry.client.domain.ClientType;
import id.co.lolita.laundry.client.domain.port.in.ManageClientUseCase.CreateClientCommand;
import id.co.lolita.laundry.client.domain.port.in.ManageDepartmentUseCase.CreateDepartmentCommand;
import id.co.lolita.laundry.client.domain.port.in.ManagePriceListUseCase.SetPriceCommand;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    ClientTypeRepository clientTypeRepository;
    @InjectMocks
    ClientService service;

    private static final long TYPE_ID = 1L;

    private Client activeClient(long id) {
        return new Client(id, "X", "X", TYPE_ID, BillingMode.COMBINED,
                null, null, null, UUID.randomUUID(), true, null);
    }

    @Test
    void createClient_rejectsDuplicateCode() {
        when(clientRepository.existsByClientCode("PBS")).thenReturn(true);
        var cmd = new CreateClientCommand("Pasar Baru", "PBS", TYPE_ID,
                BillingMode.PER_DEPARTMENT, null, null, null);

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
                BillingMode.COMBINED, "Reception", "022", "addr");

        var result = service.createClient(cmd);

        assertThat(result.getOrderToken()).isNotNull();
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void setPrice_defaultsEffectiveDateToToday_whenNull() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(activeClient(1L)));
        when(priceListRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.setPrice(new SetPriceCommand(1L, 10L, new BigDecimal("5000"), null));

        assertThat(saved.effectiveDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void setPrice_rejectsUnknownClient() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setPrice(new SetPriceCommand(99L, 10L, BigDecimal.TEN, null)))
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
    void getClientByToken_rejectsInactiveClient() {
        var token = UUID.randomUUID();
        var inactive = new Client(1L, "X", "X", TYPE_ID, BillingMode.COMBINED,
                null, null, null, token, false, null);
        when(clientRepository.findByOrderToken(token)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.getClientByToken(token))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
