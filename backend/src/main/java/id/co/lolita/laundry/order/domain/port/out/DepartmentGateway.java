package id.co.lolita.laundry.order.domain.port.out;

import java.util.List;

/**
 * Order module's view of client departments (used for the PBS per-department flow).
 */
public interface DepartmentGateway {

    record DepartmentSnapshot(Long id, String name) {
    }

    List<DepartmentSnapshot> activeForClient(Long clientId);

    boolean existsForClient(Long departmentId, Long clientId);
}