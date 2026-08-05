package id.co.lolita.laundry.settings.application;

import id.co.lolita.laundry.settings.domain.BankAccount;
import id.co.lolita.laundry.settings.domain.port.in.ManageBankAccountUseCase.CreateBankAccountCommand;
import id.co.lolita.laundry.settings.domain.port.out.BankAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The cross-row rules BankAccountService owns: exactly one default account, the default can never
 * be deactivated, and the resolve() fallback chain that keeps a billing PDF from ever rendering a
 * blank transfer block. Pure Mockito — no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {

    @Mock
    BankAccountRepository repository;
    @InjectMocks
    BankAccountService service;

    private static BankAccount personal() {
        return new BankAccount(1L, "Rekening Pribadi", "Alban Valentino Ramatir", "Bank BCA",
                "4061792362", "Lolita Laundry", true, true, 0);
    }

    private static BankAccount company() {
        return new BankAccount(2L, "Rekening Perusahaan", "PT Lolita Laundry", "Bank Mandiri",
                "1230004567", "PT Lolita Laundry", false, true, 1);
    }

    @Test
    void create_makesTheFirstAccountTheDefault() {
        when(repository.findDefault()).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var created = service.create(new CreateBankAccountCommand("Rekening Pribadi", "Alban", "Bank BCA",
                "4061792362", "Lolita Laundry", 0));

        assertThat(created.isDefaultAccount()).isTrue();
    }

    @Test
    void create_leavesLaterAccountsNonDefault() {
        when(repository.findDefault()).thenReturn(Optional.of(personal()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var created = service.create(new CreateBankAccountCommand("Rekening Perusahaan", "PT Lolita",
                "Bank Mandiri", "1230004567", "PT Lolita Laundry", 1));

        assertThat(created.isDefaultAccount()).isFalse();
    }

    @Test
    void setDefault_clearsThePreviousDefault_soExactlyOneRemains() {
        var previous = personal();
        when(repository.findById(2L)).thenReturn(Optional.of(company()));
        when(repository.findDefault()).thenReturn(Optional.of(previous));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var promoted = service.setDefault(2L);

        assertThat(promoted.isDefaultAccount()).isTrue();
        assertThat(previous.isDefaultAccount()).isFalse();
        // The clear has to be saved before the promotion — the partial unique index is checked
        // per statement, so both being true at once would fail the write.
        var order = org.mockito.Mockito.inOrder(repository);
        order.verify(repository).save(previous);
        order.verify(repository).save(promoted);
    }

    @Test
    void setDefault_isANoOpForTheAccountThatIsAlreadyDefault() {
        when(repository.findById(1L)).thenReturn(Optional.of(personal()));

        assertThat(service.setDefault(1L).isDefaultAccount()).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void setDefault_rejectsAnInactiveAccount() {
        var inactive = company();
        inactive.deactivate();
        when(repository.findById(2L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.setDefault(2L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void setActive_rejectsDeactivatingTheDefault() {
        // Every unassigned client bills to the default, so it must stay usable.
        when(repository.findById(1L)).thenReturn(Optional.of(personal()));

        assertThatThrownBy(() -> service.setActive(1L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("default");
        verify(repository, never()).save(any());
    }

    @Test
    void setActive_deactivatesANonDefaultAccount() {
        when(repository.findById(2L)).thenReturn(Optional.of(company()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.setActive(2L, false).isActive()).isFalse();
    }

    @Test
    void resolve_returnsTheAssignedAccount() {
        when(repository.findById(2L)).thenReturn(Optional.of(company()));

        assertThat(service.resolve(2L).accountNumber()).isEqualTo("1230004567");
    }

    @Test
    void resolve_fallsBackToTheDefault_forAnUnassignedClient() {
        when(repository.findDefault()).thenReturn(Optional.of(personal()));

        assertThat(service.resolve(null).accountNumber()).isEqualTo("4061792362");
    }

    @Test
    void resolve_fallsBackToTheDefault_whenTheAssignedAccountIsGone() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        when(repository.findDefault()).thenReturn(Optional.of(personal()));

        assertThat(service.resolve(99L).accountNumber()).isEqualTo("4061792362");
    }

    @Test
    void resolve_fallsBackToTheDefault_whenTheAssignedAccountIsInactive() {
        // Deactivating an assigned account is not blocked (that guard would need settings to read
        // the client module), so the render degrades instead of failing.
        var inactive = company();
        inactive.deactivate();
        when(repository.findById(2L)).thenReturn(Optional.of(inactive));
        when(repository.findDefault()).thenReturn(Optional.of(personal()));

        assertThat(service.resolve(2L).accountNumber()).isEqualTo("4061792362");
    }

    @Test
    void resolve_fallsBackToBuiltInDefaults_whenNoAccountExistsAtAll() {
        when(repository.findDefault()).thenReturn(Optional.empty());

        // Never empty — a billing PDF must not render a blank transfer block.
        assertThat(service.resolve(null).accountNumber())
                .isEqualTo(BankAccount.defaults().getAccountNumber());
    }
}
