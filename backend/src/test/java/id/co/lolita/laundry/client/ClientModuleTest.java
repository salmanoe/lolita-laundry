package id.co.lolita.laundry.client;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;

/**
 * Bootstraps only the client module in isolation.
 *
 * <p>Verifies that Client, Department, and ClientPriceList adapters are all wired
 * correctly through their port interfaces, and that no internal class from another
 * module (e.g. catalog's ItemJpaEntity) is imported directly.
 *
 * <p>Note: ClientService holds references to item IDs as plain {@code Long} values —
 * it does NOT import ItemMaster from the catalog module. The cross-module data
 * flow is intentionally ID-based.
 */
@ApplicationModuleTest
class ClientModuleTest {

    @Test
    void clientModuleBootstrapsInIsolation() {
        // Context loading IS the assertion.
    }
}
