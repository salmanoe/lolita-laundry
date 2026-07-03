package id.co.lolita.laundry.user.application;

import id.co.lolita.laundry.shared.NotFoundException;
import id.co.lolita.laundry.user.domain.PendingUser;
import id.co.lolita.laundry.user.domain.Role;
import id.co.lolita.laundry.user.domain.User;
import id.co.lolita.laundry.user.domain.port.in.LoadUserByAuth0SubUseCase;
import id.co.lolita.laundry.user.domain.port.in.ManageUserUseCase;
import id.co.lolita.laundry.user.domain.port.in.SelfRegisterUseCase;
import id.co.lolita.laundry.user.domain.port.in.UserDirectoryQuery;
import id.co.lolita.laundry.user.domain.port.out.PendingUserRepository;
import id.co.lolita.laundry.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class UserService implements LoadUserByAuth0SubUseCase, UserDirectoryQuery, ManageUserUseCase,
        SelfRegisterUseCase {

    private final UserRepository userRepository;
    private final PendingUserRepository pendingUserRepository;

    @Override
    public Optional<User> loadByAuth0Sub(String auth0Sub) {
        return userRepository.findByAuth0Sub(auth0Sub);
    }

    @Override
    public Optional<Long> idForAuth0Sub(String auth0Sub) {
        return userRepository.findByAuth0Sub(auth0Sub).map(User::getId);
    }

    @Override
    public List<User> list() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User create(CreateUserCommand command) {
        var user = User.register(command.auth0Sub(), null, command.fullName(), command.role());
        if (userRepository.existsByAuth0Sub(user.getAuth0Sub())) {
            throw new IllegalArgumentException("Pengguna dengan Auth0 sub ini sudah terdaftar");
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User update(UpdateUserCommand command) {
        var user = load(command.id());
        // Demoting the only active SUPER_ADMIN would lock everyone out of user administration.
        if (user.getRole() == Role.SUPER_ADMIN && command.role() != Role.SUPER_ADMIN && isLastActiveSuperAdmin(user)) {
            throw new IllegalArgumentException("Tidak dapat mengubah peran SUPER_ADMIN aktif terakhir");
        }
        user.rename(command.fullName());
        user.changeRole(command.role());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User setActive(Long id, boolean active) {
        var user = load(id);
        if (!active && user.getRole() == Role.SUPER_ADMIN && isLastActiveSuperAdmin(user)) {
            throw new IllegalArgumentException("Tidak dapat menonaktifkan SUPER_ADMIN aktif terakhir");
        }
        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void selfRegister(String auth0Sub, String email, String fullName) {
        var sub = auth0Sub == null ? "" : auth0Sub.trim();
        if (sub.isEmpty()) {
            throw new IllegalArgumentException("Auth0 sub wajib diisi");
        }
        // Already a real user → nothing to queue. Already queued → refresh the profile hints.
        if (userRepository.existsByAuth0Sub(sub)) {
            return;
        }
        var pending = pendingUserRepository.findByAuth0Sub(sub)
                .map(existing -> new PendingUser(existing.id(), sub, trimToNull(email), trimToNull(fullName),
                        existing.requestedAt()))
                .orElseGet(() -> PendingUser.of(sub, email, fullName));
        pendingUserRepository.save(pending);
    }

    @Override
    public List<PendingUser> listPending() {
        return pendingUserRepository.findAll();
    }

    @Override
    @Transactional
    public User approve(Long pendingId, Role role) {
        var pending = pendingUserRepository.findById(pendingId)
                .orElseThrow(() -> new NotFoundException("Permintaan akses tidak ditemukan: " + pendingId));
        if (userRepository.existsByAuth0Sub(pending.auth0Sub())) {
            // Defensive: someone provisioned this sub manually in the meantime. Clear the stale request.
            pendingUserRepository.deleteById(pendingId);
            throw new IllegalArgumentException("Pengguna dengan Auth0 sub ini sudah terdaftar");
        }
        // Duplicate-identity guard: this email already belongs to a provisioned user, so this pending
        // request is the same person returning under a second Auth0 sub (e.g. Google vs email+password).
        // Approving would create a users row for a sub they aren't logged in as → they'd stay stuck on
        // the pending screen forever. Refuse and keep the request so the SUPER_ADMIN can reconcile the
        // duplicate (link the Auth0 accounts / update the existing user's sub) instead of silently
        // splitting the identity. The pending row is left in place on purpose (unlike the sub branch).
        if (pending.email() != null && userRepository.existsByEmailIgnoreCase(pending.email())) {
            throw new IllegalArgumentException(
                    "Email " + pending.email() + " sudah dipakai pengguna lain — kemungkinan akun login ganda "
                            + "(Google + email/kata sandi). Periksa daftar Pengguna dan satukan akunnya sebelum menyetujui.");
        }
        var user = userRepository.save(
                User.register(pending.auth0Sub(), pending.email(), approvedName(pending), role));
        pendingUserRepository.deleteById(pendingId);
        return user;
    }

    @Override
    @Transactional
    public void rejectPending(Long pendingId) {
        if (pendingUserRepository.findById(pendingId).isEmpty()) {
            throw new NotFoundException("Permintaan akses tidak ditemukan: " + pendingId);
        }
        pendingUserRepository.deleteById(pendingId);
    }

    /**
     * A user row needs a non-blank name; fall back to email then sub when the profile gave none.
     */
    private static String approvedName(PendingUser pending) {
        if (pending.fullName() != null) {
            return pending.fullName();
        }
        return pending.email() != null ? pending.email() : pending.auth0Sub();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private User load(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pengguna tidak ditemukan: " + id));
    }

    /**
     * True when {@code target} is the only currently-active SUPER_ADMIN.
     */
    private boolean isLastActiveSuperAdmin(User target) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN && u.isActive())
                .allMatch(u -> u.getId().equals(target.getId()));
    }
}
