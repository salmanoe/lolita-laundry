package id.co.lolita.laundry.user.domain.port.out;

import id.co.lolita.laundry.user.domain.PendingUser;

import java.util.List;
import java.util.Optional;

public interface PendingUserRepository {

    List<PendingUser> findAll();

    Optional<PendingUser> findById(Long id);

    Optional<PendingUser> findByAuth0Sub(String auth0Sub);

    PendingUser save(PendingUser pendingUser);

    void deleteById(Long id);

    void deleteByAuth0Sub(String auth0Sub);
}
