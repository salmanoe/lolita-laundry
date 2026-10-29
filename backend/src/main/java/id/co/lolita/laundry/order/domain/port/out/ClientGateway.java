package id.co.lolita.laundry.order.domain.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Order module's view of the {@code client} module. The adapter delegates to the client
 * module's exposed query API; the order domain sees only this snapshot.
 */
public interface ClientGateway {

    /**
     * @param treatmentEnabled whether this client's orders may use Treatment (×2) pricing —
     *                         independent of {@code perDepartment}
     */
    record ClientSnapshot(Long id, String name, String clientCode, boolean active, boolean perDepartment,
                          boolean treatmentEnabled) {
    }

    Optional<ClientSnapshot> findByToken(UUID token);

    Optional<ClientSnapshot> findById(Long clientId);
}