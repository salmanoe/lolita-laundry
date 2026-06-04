package id.co.lolita.laundry.client.domain.port.in;

import id.co.lolita.laundry.client.domain.Department;

import java.util.List;

public interface ManageDepartmentUseCase {

    record CreateDepartmentCommand(Long clientId, String name) {
    }

    record UpdateDepartmentCommand(Long id, String name, boolean active) {
    }

    List<Department> getDepartmentsByClient(Long clientId);

    Department createDepartment(CreateDepartmentCommand command);

    Department updateDepartment(UpdateDepartmentCommand command);
}
