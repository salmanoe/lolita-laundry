package id.co.lolita.laundry.user.domain.port.in;

import id.co.lolita.laundry.user.domain.User;

import java.util.Optional;

public interface LoadUserByAuth0SubUseCase {
    Optional<User> loadByAuth0Sub(String auth0Sub);
}
