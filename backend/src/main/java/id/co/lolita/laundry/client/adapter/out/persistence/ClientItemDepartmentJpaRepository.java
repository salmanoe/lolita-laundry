package id.co.lolita.laundry.client.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ClientItemDepartmentJpaRepository extends JpaRepository<ClientItemDepartmentJpaEntity, Long> {

    List<ClientItemDepartmentJpaEntity> findByClientId(Long clientId);

    Optional<ClientItemDepartmentJpaEntity> findByClientIdAndItemId(Long clientId, Long itemId);
}