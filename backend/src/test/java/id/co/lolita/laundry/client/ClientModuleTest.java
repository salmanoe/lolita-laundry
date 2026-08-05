package id.co.lolita.laundry.client;

import id.co.lolita.laundry.settings.domain.port.in.BankAccountQuery;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
 *
 * <p>The one genuine cross-module read is the settings module's {@code BankAccountQuery}
 * (named interface {@code settings::api}), used to validate a client's bank-account assignment.
 * That provider bean lives in a module not started here, so it is supplied as a mock.
 */
@ApplicationModuleTest
class ClientModuleTest {

    @MockitoBean
    BankAccountQuery bankAccountQuery;

    @Test
    void clientModuleBootstrapsInIsolation() {
        // Context loading IS the assertion.
    }
}
