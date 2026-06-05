package id.co.lolita.laundry.user.application;

import id.co.lolita.laundry.user.domain.Role;
import id.co.lolita.laundry.user.domain.User;
import id.co.lolita.laundry.user.domain.port.in.LoadUserByAuth0SubUseCase;
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
class UserService implements LoadUserByAuth0SubUseCase, UserDirectoryQuery {

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
    public List<DriverSummary> activeDrivers() {
        return userRepository.findActiveByRole(Role.DRIVER).stream()
                .map(u -> new DriverSummary(u.getId(), u.getFullName()))
                .toList();
    }

    @Override
    public boolean isActiveDriver(Long userId) {
        return userId != null && userRepository.findById(userId)
                .filter(User::isActive)
                .map(u -> u.getRole() == Role.DRIVER)
                .orElse(false);
    }
}
