package id.co.lolita.laundry.user.domain.port.out;

import id.co.lolita.laundry.user.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByAuth0Sub(String auth0Sub);

    Optional<User> findById(Long id);

    List<User> findAll();

    boolean existsByAuth0Sub(String auth0Sub);

    /**
     * True when any user already carries this email (case-insensitive). Used by the approval flow
     * to spot a duplicate identity — the same person returning under a second Auth0 sub.
     */
    boolean existsByEmailIgnoreCase(String email);

    User save(User user);
}
