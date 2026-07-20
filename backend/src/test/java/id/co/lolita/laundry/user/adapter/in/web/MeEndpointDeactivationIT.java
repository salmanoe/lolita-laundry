package id.co.lolita.laundry.user.adapter.in.web;

import id.co.lolita.laundry.user.domain.Role;
import id.co.lolita.laundry.user.domain.User;
import id.co.lolita.laundry.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runtime proof (full Spring context + real {@code SecurityConfig}, JWT enforced) of the revoke
 * contract on {@code GET /api/me}: a deactivated user is cut off with 403 {@code account_deactivated},
 * an active user gets their profile, an unprovisioned sub gets 204. Complements the pure-unit
 * {@code Auth0JwtAuthenticationConverterTest}. The running dev instance can't exercise this (dev
 * profile permits all with no JWT), so this booted test is the pre-deploy runtime check.
 *
 * <p>MockMvc is built from the {@link WebApplicationContext} (no {@code @AutoConfigureMockMvc}) so it
 * needs only {@code spring-test} + {@code spring-security-test}, avoiding a Boot-4 web test-slice dep.
 */
@SpringBootTest
class MeEndpointDeactivationIT {

    @Autowired
    WebApplicationContext context;

    @Autowired
    UserRepository users;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void deactivatedUser_isCutOffWith403() throws Exception {
        var deactivated = User.register("auth0|inactive-me", "inactive@lolita.co.id", "Inactive User", Role.FINANCE_STAFF);
        deactivated.deactivate();
        users.save(deactivated);

        mvc.perform(get("/api/me").with(jwt().jwt(j -> j.subject("auth0|inactive-me"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("account_deactivated"));
    }

    @Test
    void activeUser_getsProfile() throws Exception {
        users.save(User.register("auth0|active-me", "active@lolita.co.id", "Active User", Role.SUPER_ADMIN));

        mvc.perform(get("/api/me").with(jwt().jwt(j -> j.subject("auth0|active-me"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"));
    }

    @Test
    void unprovisionedSub_gets204() throws Exception {
        mvc.perform(get("/api/me").with(jwt().jwt(j -> j.subject("auth0|never-seen"))))
                .andExpect(status().isNoContent());
    }
}
