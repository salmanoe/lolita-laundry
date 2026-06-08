package id.co.lolita.laundry.settings;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;

/**
 * Bootstraps only the settings module in isolation.
 *
 * <p>If the context loads, the CompanyProfile inbound ports → application service → outbound
 * repository → JPA adapter chain is wired, and the {@code CompanyProfileController} resolves its
 * dependencies via port interfaces only. The cross-module {@code settings::api} named interface
 * ({@code CompanyProfileQuery}) is verified structurally by {@code ModuleStructureTest}.
 */
@ApplicationModuleTest
class SettingsModuleTest {

    @Test
    void settingsModuleBootstrapsInIsolation() {
        // Context loading IS the assertion.
    }
}