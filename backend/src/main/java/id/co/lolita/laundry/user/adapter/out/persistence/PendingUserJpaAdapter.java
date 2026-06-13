package id.co.lolita.laundry.user.adapter.out.persistence;

import id.co.lolita.laundry.user.domain.PendingUser;
import id.co.lolita.laundry.user.domain.port.out.PendingUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PendingUserJpaAdapter implements PendingUserRepository {

    private final PendingUserJpaRepository jpaRepository;

    @Override
    public List<PendingUser> findAll() {
        return jpaRepository.findAll(Sort.by(Sort.Direction.ASC, "requestedAt")).stream()
                .map(PendingUserJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<PendingUser> findById(Long id) {
        return jpaRepository.findById(id).map(PendingUserJpaEntity::toDomain);
    }

    @Override
    public Optional<PendingUser> findByAuth0Sub(String auth0Sub) {
        return jpaRepository.findByAuth0Sub(auth0Sub).map(PendingUserJpaEntity::toDomain);
    }

    @Override
    public PendingUser save(PendingUser pendingUser) {
        return jpaRepository.save(PendingUserJpaEntity.fromDomain(pendingUser)).toDomain();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByAuth0Sub(String auth0Sub) {
        jpaRepository.deleteByAuth0Sub(auth0Sub);
    }
}
