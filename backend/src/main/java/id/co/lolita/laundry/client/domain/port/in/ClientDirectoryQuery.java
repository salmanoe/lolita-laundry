package id.co.lolita.laundry.client.domain.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only directory lookups other modules need about clients and their departments.
 *
 * <p>Exposed cross-module (named interface "api"). Returns self-contained records — no
 * client domain types leak across the boundary.
 */
public interface ClientDirectoryQuery {

    record ClientView(Long id, String name, String clientCode, boolean active, boolean perDepartment) {
    }

    record DepartmentView(Long id, String name) {
    }

    /**
     * Looks up a client by its public order token (active and inactive — caller decides).
     */
    Optional<ClientView> findByToken(UUID token);

    Optional<ClientView> findById(Long clientId);

    /**
     * Active departments for a client (empty unless the client bills per department).
     */
    List<DepartmentView> activeDepartments(Long clientId);

    /**
     * Every department for a client, active <em>and</em> inactive. Used to label historical data
     * (delivered orders, invoices, monthly billings) so a department deactivated after the fact
     * still resolves its name — departments are soft-deactivated, never deleted.
     */
    List<DepartmentView> allDepartments(Long clientId);

    boolean departmentBelongsToClient(Long departmentId, Long clientId);
}
