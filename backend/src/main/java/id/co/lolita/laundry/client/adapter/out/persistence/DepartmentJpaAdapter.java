package id.co.lolita.laundry.client.adapter.out.persistence;

import id.co.lolita.laundry.client.domain.Department;
import id.co.lolita.laundry.client.domain.port.out.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class DepartmentJpaAdapter implements DepartmentRepository {

    private final DepartmentJpaRepository jpaRepository;

    @Override
    public List<Department> findByClientId(Long clientId) {
        return jpaRepository.findByClientId(clientId).stream().map(DepartmentJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<Department> findById(Long id) {
        return jpaRepository.findById(id).map(DepartmentJpaEntity::toDomain);
    }

    @Override
    public Department save(Department department) {
        return jpaRepository.save(DepartmentJpaEntity.fromDomain(department)).toDomain();
    }
}
