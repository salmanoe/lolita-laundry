package id.co.lolita.laundry.order.adapter.out.gateway;

import id.co.lolita.laundry.client.domain.port.in.ClientDirectoryQuery;
import id.co.lolita.laundry.order.domain.port.out.DepartmentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class DepartmentGatewayAdapter implements DepartmentGateway {

    private final ClientDirectoryQuery directory;

    @Override
    public List<DepartmentSnapshot> activeForClient(Long clientId) {
        return directory.activeDepartments(clientId).stream()
                .map(d -> new DepartmentSnapshot(d.id(), d.name()))
                .toList();
    }

    @Override
    public List<DepartmentSnapshot> allForClient(Long clientId) {
        return directory.allDepartments(clientId).stream()
                .map(d -> new DepartmentSnapshot(d.id(), d.name()))
                .toList();
    }

    @Override
    public boolean existsForClient(Long departmentId, Long clientId) {
        return directory.departmentBelongsToClient(departmentId, clientId);
    }
}