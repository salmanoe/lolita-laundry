package id.co.lolita.laundry.user.domain.port.in;

import id.co.lolita.laundry.user.domain.PendingUser;
import id.co.lolita.laundry.user.domain.Role;
import id.co.lolita.laundry.user.domain.User;

import java.util.List;

/**
 * SUPER_ADMIN-facing user administration (Phase 5). The Auth0 account is created out-of-band
 * (Google social or email+password); this manages the matching local {@code users} row and the
 * pending self-registration queue ({@link #listPending()} / {@link #approve} / {@link #rejectPending}).
 */
public interface ManageUserUseCase {

    List<User> list();

    User create(CreateUserCommand command);

    User update(UpdateUserCommand command);

    /**
     * Activate or deactivate a user. Deactivating the last active SUPER_ADMIN is rejected so the
     * super admin can never lock themselves out.
     */
    User setActive(Long id, boolean active);

    /**
     * Self-registered identities awaiting approval (oldest first).
     */
    List<PendingUser> listPending();

    /**
     * Approve a pending registration: create the real {@code users} row with the chosen role and
     * clear the pending entry. Rejected if the sub is already provisioned.
     */
    User approve(Long pendingId, Role role);

    /**
     * Reject (discard) a pending registration without creating a user.
     */
    void rejectPending(Long pendingId);

    record CreateUserCommand(String auth0Sub, String fullName, Role role) {
    }

    record UpdateUserCommand(Long id, String fullName, Role role) {
    }
}