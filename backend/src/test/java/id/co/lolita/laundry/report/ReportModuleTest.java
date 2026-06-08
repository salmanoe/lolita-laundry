package id.co.lolita.laundry.report;

import id.co.lolita.laundry.client.domain.port.in.ClientDirectoryQuery;
import id.co.lolita.laundry.order.domain.port.in.OrderReportQuery;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Bootstraps only the report module in isolation. Report reads the order and client modules
 * through their exposed named interfaces; those provider beans live in modules that are NOT
 * started here, so they are supplied as mocks. If the context loads, the
 * controller → inbound port → service → outbound gateway → adapter wiring is correct and no
 * report class reaches into another module's internals.
 */
@ApplicationModuleTest
class ReportModuleTest {

    @MockitoBean
    OrderReportQuery orderReportQuery;
    @MockitoBean
    ClientDirectoryQuery clientDirectoryQuery;
    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void reportModuleBootstrapsInIsolation() {
        // Context loading IS the assertion.
    }
}