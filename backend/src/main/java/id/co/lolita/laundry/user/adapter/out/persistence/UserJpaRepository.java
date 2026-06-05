package id.co.lolita.laundry.user.adapter.out.persistence;

import id.co.lolita.laundry.user.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByAuth0Sub(String auth0Sub);

    List<UserJpaEntity> findByRoleAndActiveTrueOrderByFullNameAsc(Role role);
}
