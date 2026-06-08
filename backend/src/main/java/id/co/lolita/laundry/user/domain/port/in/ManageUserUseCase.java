package id.co.lolita.laundry.user.domain.port.in;

import id.co.lolita.laundry.user.domain.Role;
import id.co.lolita.laundry.user.domain.User;

import java.util.List;

/**
 * Owner-facing user administration (Phase 5). Replaces the manual SQL seed of staff/driver
 * rows: the owner still creates the Auth0 account, then registers it here with its {@code sub}.
 */
public interface ManageUserUseCase {

    List<User> list();

    User create(CreateUserCommand command);

    User update(UpdateUserCommand command);

    /**
     * Activate or deactivate a user. Deactivating the last active OWNER is rejected so the
     * owner can never lock themselves out.
     */
    User setActive(Long id, boolean active);

    record CreateUserCommand(String auth0Sub, String fullName, Role role) {
    }

    record UpdateUserCommand(Long id, String fullName, Role role) {
    }
}