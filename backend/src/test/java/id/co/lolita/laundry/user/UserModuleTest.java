package id.co.lolita.laundry.user;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Bootstraps only the user module in isolation.
 *
 * <p>The user module owns security configuration ({@code SecurityConfig},
 * {@code Auth0JwtAuthenticationConverter}). The JWT resource server is configured via
 * {@code jwk-set-uri} in the test profile, which defers JWK fetching until an actual
 * JWT arrives (never during this test). The {@link JwtDecoder} mock below provides
 * an additional safety net — it prevents any auto-configured decoder from making
 * network calls if auto-configuration discovers the property before our bean does.
 *
 * <p>Note: Spring Boot 4.0 replaced {@code @MockBean} with {@code @MockitoBean}
 * (package {@code org.springframework.test.context.bean.override.mockito}).
 */
@ApplicationModuleTest
class UserModuleTest {

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void userModuleBootstrapsInIsolation() {
        // Context loading IS the assertion.
        // SecurityConfig, Auth0JwtAuthenticationConverter, UserService, and UserJpaAdapter
        // must all wire correctly within the module boundary.
    }
}
