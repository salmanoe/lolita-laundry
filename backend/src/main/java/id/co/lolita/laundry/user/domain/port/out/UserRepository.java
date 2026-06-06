package id.co.lolita.laundry.user.domain.port.out;

import id.co.lolita.laundry.user.domain.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByAuth0Sub(String auth0Sub);

    Optional<User> findById(Long id);

    User save(User user);
}
