package id.co.lolita.laundry.client.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface DepartmentJpaRepository extends JpaRepository<DepartmentJpaEntity, Long> {
    List<DepartmentJpaEntity> findByClientId(Long clientId);
}
