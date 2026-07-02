package id.co.lolita.laundry.order;

import id.co.lolita.laundry.catalog.domain.port.in.CatalogQuery;
import id.co.lolita.laundry.client.domain.port.in.ClientDirectoryQuery;
import id.co.lolita.laundry.client.domain.port.in.ClientPricingQuery;
import id.co.lolita.laundry.order.domain.port.out.billing.BillingStatusPort;
import id.co.lolita.laundry.storage.domain.port.out.StoragePort;
import id.co.lolita.laundry.user.domain.port.in.UserDirectoryQuery;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Bootstraps only the order module in isolation. The order module reads other modules
 * through their exposed named interfaces; those provider beans live in modules that are
 * NOT started here, so they are supplied as mocks. If the context loads, the inbound port
 * → application service → outbound port/gateway → adapter wiring is all correct and no
 * order class reaches into another module's internals.
 */
@ApplicationModuleTest
class OrderModuleTest {

    @MockitoBean ClientDirectoryQuery clientDirectoryQuery;
    @MockitoBean ClientPricingQuery clientPricingQuery;
    @MockitoBean CatalogQuery catalogQuery;
    @MockitoBean UserDirectoryQuery userDirectoryQuery;
    @MockitoBean StoragePort storagePort;
    // Order-owned SPI implemented by the billing module (not started in this isolated test).
    @MockitoBean BillingStatusPort billingStatusPort;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void orderModuleBootstrapsInIsolation() {
        // Context loading IS the assertion.
    }
}