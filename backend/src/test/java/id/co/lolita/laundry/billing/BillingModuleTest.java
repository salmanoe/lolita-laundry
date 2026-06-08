package id.co.lolita.laundry.billing;

import id.co.lolita.laundry.client.domain.port.in.ClientDirectoryQuery;
import id.co.lolita.laundry.order.domain.port.in.DeliveredOrderQuery;
import id.co.lolita.laundry.settings.domain.port.in.CompanyProfileQuery;
import id.co.lolita.laundry.storage.domain.port.out.StoragePort;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Bootstraps only the billing module in isolation. Billing reads the order, client and
 * storage modules through their exposed named interfaces; those provider beans live in
 * modules that are NOT started here, so they are supplied as mocks. If the context loads, the
 * event listener → inbound port → application service → outbound port/gateway → adapter wiring
 * (including the JasperReports PDF adapter) is all correct and no billing class reaches into
 * another module's internals.
 */
@ApplicationModuleTest
class BillingModuleTest {

    @MockitoBean DeliveredOrderQuery deliveredOrderQuery;
    @MockitoBean ClientDirectoryQuery clientDirectoryQuery;
    @MockitoBean CompanyProfileQuery companyProfileQuery;
    @MockitoBean StoragePort storagePort;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void billingModuleBootstrapsInIsolation() {
        // Context loading IS the assertion.
    }
}