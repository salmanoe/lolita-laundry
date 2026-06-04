package id.co.lolita.laundry.user.adapter.out.persistence;

import id.co.lolita.laundry.user.domain.User;
import id.co.lolita.laundry.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class UserJpaAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    @Override
    public Optional<User> findByAuth0Sub(String auth0Sub) {
        return jpaRepository.findByAuth0Sub(auth0Sub).map(UserJpaEntity::toDomain);
    }

    @Override
    public User save(User user) {
        return jpaRepository.save(UserJpaEntity.fromDomain(user)).toDomain();
    }
}
