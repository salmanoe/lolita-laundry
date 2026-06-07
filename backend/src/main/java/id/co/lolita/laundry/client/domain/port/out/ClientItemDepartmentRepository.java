package id.co.lolita.laundry.client.domain.port.out;

import id.co.lolita.laundry.client.domain.ClientItemDepartment;

import java.util.List;
import java.util.Optional;

public interface ClientItemDepartmentRepository {

    /** Every item→department mapping for a client. */
    List<ClientItemDepartment> findByClient(Long clientId);

    /** The mapping for one item for one client, if assigned. */
    Optional<ClientItemDepartment> find(Long clientId, Long itemId);

    /** Inserts or updates the mapping in place (one department per item per client). */
    ClientItemDepartment upsert(Long clientId, Long itemId, Long departmentId);

    /** Removes the mapping for an item (e.g. when a client switches to COMBINED billing). */
    void delete(Long clientId, Long itemId);
}