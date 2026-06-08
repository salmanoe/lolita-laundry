package id.co.lolita.laundry.user.application;

import id.co.lolita.laundry.shared.NotFoundException;
import id.co.lolita.laundry.user.domain.Role;
import id.co.lolita.laundry.user.domain.User;
import id.co.lolita.laundry.user.domain.port.in.LoadUserByAuth0SubUseCase;
import id.co.lolita.laundry.user.domain.port.in.ManageUserUseCase;
import id.co.lolita.laundry.user.domain.port.in.UserDirectoryQuery;
import id.co.lolita.laundry.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class UserService implements LoadUserByAuth0SubUseCase, UserDirectoryQuery, ManageUserUseCase {

    private final UserRepository userRepository;

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
        var user = User.register(command.auth0Sub(), command.fullName(), command.role());
        if (userRepository.existsByAuth0Sub(user.getAuth0Sub())) {
            throw new IllegalArgumentException("Pengguna dengan Auth0 sub ini sudah terdaftar");
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User update(UpdateUserCommand command) {
        var user = load(command.id());
        // Demoting the only active OWNER would leave the system without an administrator.
        if (user.getRole() == Role.OWNER && command.role() != Role.OWNER && isLastActiveOwner(user)) {
            throw new IllegalArgumentException("Tidak dapat mengubah peran OWNER aktif terakhir");
        }
        user.rename(command.fullName());
        user.changeRole(command.role());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User setActive(Long id, boolean active) {
        var user = load(id);
        if (!active && user.getRole() == Role.OWNER && isLastActiveOwner(user)) {
            throw new IllegalArgumentException("Tidak dapat menonaktifkan OWNER aktif terakhir");
        }
        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }
        return userRepository.save(user);
    }

    private User load(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pengguna tidak ditemukan: " + id));
    }

    /**
     * True when {@code target} is the only currently-active OWNER.
     */
    private boolean isLastActiveOwner(User target) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.OWNER && u.isActive())
                .allMatch(u -> u.getId().equals(target.getId()));
    }
}
