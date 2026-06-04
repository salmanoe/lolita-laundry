package id.co.lolita.laundry.client.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ClientJpaRepository extends JpaRepository<ClientJpaEntity, Long> {
    List<ClientJpaEntity> findByActiveTrue();

    Optional<ClientJpaEntity> findByOrderToken(UUID token);

    boolean existsByClientCode(String clientCode);
}
