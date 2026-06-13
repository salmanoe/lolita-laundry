package id.co.lolita.laundry.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface PendingUserJpaRepository extends JpaRepository<PendingUserJpaEntity, Long> {
    Optional<PendingUserJpaEntity> findByAuth0Sub(String auth0Sub);

    void deleteByAuth0Sub(String auth0Sub);
}
