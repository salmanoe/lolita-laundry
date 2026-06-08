package id.co.lolita.laundry.report.domain.port.out;

import java.util.Optional;

/**
 * Report's view of client directory data, used to label report rows with client names/codes.
 * The adapter delegates to the client module's {@code ClientDirectoryQuery}
 * (named interface {@code client::api}).
 */
public interface ClientLookupGateway {

    record ClientInfo(Long id, String name, String clientCode) {
    }

    Optional<ClientInfo> findById(Long clientId);
}
