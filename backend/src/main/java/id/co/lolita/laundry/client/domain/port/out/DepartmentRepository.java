package id.co.lolita.laundry.client.domain.port.out;

import id.co.lolita.laundry.client.domain.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository {
    List<Department> findByClientId(Long clientId);

    Optional<Department> findById(Long id);

    Department save(Department department);
}
