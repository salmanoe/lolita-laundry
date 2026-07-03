package id.co.lolita.laundry.user.application;

import id.co.lolita.laundry.shared.NotFoundException;
import id.co.lolita.laundry.user.domain.PendingUser;
import id.co.lolita.laundry.user.domain.Role;
import id.co.lolita.laundry.user.domain.User;
import id.co.lolita.laundry.user.domain.port.in.ManageUserUseCase.CreateUserCommand;
import id.co.lolita.laundry.user.domain.port.in.ManageUserUseCase.UpdateUserCommand;
import id.co.lolita.laundry.user.domain.port.out.PendingUserRepository;
import id.co.lolita.laundry.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orchestration rules of UserService that aren't visible in the domain object:
 * duplicate-sub rejection on create, and the "last active SUPER_ADMIN" lock-out guard on
 * deactivate / role-demote. Pure Mockito — no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PendingUserRepository pendingUserRepository;
    @InjectMocks
    UserService service;

    private static User user(long id, Role role, boolean active) {
        return new User(id, "auth0|" + id, "user" + id + "@lolita.co.id", "User " + id, role, active, Instant.now());
    }

    @Test
    void create_rejectsDuplicateAuth0Sub() {
        when(userRepository.existsByAuth0Sub("auth0|new")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateUserCommand("auth0|new", "Budi", Role.DAILY_STAFF)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sudah terdaftar");
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_persistsNewDailyStaff() {
        when(userRepository.existsByAuth0Sub("auth0|new")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var created = service.create(new CreateUserCommand(" auth0|new ", " Joko ", Role.DAILY_STAFF));

        assertThat(created.getAuth0Sub()).isEqualTo("auth0|new");
        assertThat(created.getFullName()).isEqualTo("Joko");
        assertThat(created.getRole()).isEqualTo(Role.DAILY_STAFF);
        assertThat(created.isActive()).isTrue();
    }

    @Test
    void approve_rejectsWhenEmailAlreadyBelongsToAnotherUser() {
        // Same person returning under a second Auth0 sub: their email is already provisioned, so
        // approving would split the identity and strand them on the pending screen. Refuse instead,
        // and keep the pending row so the admin can reconcile.
        var pending = new PendingUser(5L, "google-oauth2|new", "budi@lolita.co.id", "Budi", Instant.now());
        when(pendingUserRepository.findById(5L)).thenReturn(Optional.of(pending));
        when(userRepository.existsByAuth0Sub("google-oauth2|new")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("budi@lolita.co.id")).thenReturn(true);

        assertThatThrownBy(() -> service.approve(5L, Role.DAILY_STAFF))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("akun login ganda");
        verify(userRepository, never()).save(any());
        verify(pendingUserRepository, never()).deleteById(any());
    }

    @Test
    void approve_createsUserAndClearsPendingRow() {
        var pending = new PendingUser(6L, "auth0|new", "siti@lolita.co.id", "Siti", Instant.now());
        when(pendingUserRepository.findById(6L)).thenReturn(Optional.of(pending));
        when(userRepository.existsByAuth0Sub("auth0|new")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("siti@lolita.co.id")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var created = service.approve(6L, Role.FINANCE_STAFF);

        assertThat(created.getAuth0Sub()).isEqualTo("auth0|new");
        assertThat(created.getEmail()).isEqualTo("siti@lolita.co.id");
        assertThat(created.getRole()).isEqualTo(Role.FINANCE_STAFF);
        verify(pendingUserRepository).deleteById(6L);
    }

    @Test
    void setActive_rejectsDeactivatingLastActiveSuperAdmin() {
        var admin = user(1, Role.SUPER_ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(List.of(admin, user(2, Role.FINANCE_STAFF, true)));

        assertThatThrownBy(() -> service.setActive(1L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPER_ADMIN aktif terakhir");
        verify(userRepository, never()).save(any());
    }

    @Test
    void setActive_allowsDeactivatingSuperAdminWhenAnotherActiveSuperAdminExists() {
        var admin = user(1, Role.SUPER_ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(List.of(admin, user(2, Role.SUPER_ADMIN, true)));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.setActive(1L, false);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void setActive_allowsDeactivatingLastNonAdmin() {
        // Only SUPER_ADMIN is guarded; a FINANCE_STAFF (or DAILY_STAFF) is freely deactivatable.
        var staff = user(1, Role.FINANCE_STAFF, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(staff));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.setActive(1L, false);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void update_rejectsDemotingLastActiveSuperAdmin() {
        var admin = user(1, Role.SUPER_ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(List.of(admin));

        assertThatThrownBy(() -> service.update(new UpdateUserCommand(1L, "Admin", Role.FINANCE_STAFF)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPER_ADMIN aktif terakhir");
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_renamesAndChangesRole() {
        var staff = user(3, Role.FINANCE_STAFF, true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.update(new UpdateUserCommand(3L, "Siti", Role.DAILY_STAFF));

        assertThat(result.getFullName()).isEqualTo("Siti");
        assertThat(result.getRole()).isEqualTo(Role.DAILY_STAFF);
    }

    @Test
    void update_missingUser_throwsNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(new UpdateUserCommand(9L, "X", Role.FINANCE_STAFF)))
                .isInstanceOf(NotFoundException.class);
    }
}