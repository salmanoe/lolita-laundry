package id.co.lolita.laundry.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;

/**
 * Bootstraps only the catalog module in isolation.
 *
 * <p>Spring Modulith 2.0 (requires Spring Boot 4.x) starts a minimal Spring context
 * that includes only the catalog module's beans. If the context loads, all of the
 * following are verified:
 * <ul>
 *   <li>No catalog class imports internals from another module
 *   <li>The inbound port → application service → outbound port → JPA adapter chain is wired
 *   <li>The REST controller can resolve its dependencies (all via port interfaces)
 * </ul>
 */
@ApplicationModuleTest
class CatalogModuleTest {

    @Test
    void catalogModuleBootstrapsInIsolation() {
        // Context loading IS the assertion.
        // A failed import of another module's internal class fails the context here.
    }
}
