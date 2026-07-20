package id.co.lolita.laundry.user.adapter.in.security;

import id.co.lolita.laundry.user.domain.Role;
import id.co.lolita.laundry.user.domain.User;
import id.co.lolita.laundry.user.domain.port.in.LoadUserByAuth0SubUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * The converter is the server-side revoke lever: a deactivated user's still-valid Auth0 token maps
 * to <b>no</b> authorities, so every {@code @PreAuthorize hasRole(...)} endpoint 403s on the next
 * request. An unknown sub (first login, not yet provisioned) is also authority-free but authenticates
 * so it can hit {@code /api/me} + self-register.
 */
@ExtendWith(MockitoExtension.class)
class Auth0JwtAuthenticationConverterTest {

    @Mock
    LoadUserByAuth0SubUseCase loadUser;

    private static Jwt jwt(String sub) {
        return Jwt.withTokenValue("token").header("alg", "none").subject(sub).build();
    }

    private static User user(String sub, Role role, boolean active) {
        return new User(1L, sub, "u@lolita.co.id", "User", role, active, Instant.now());
    }

    @Test
    void activeUser_getsRoleAuthority() {
        when(loadUser.loadByAuth0Sub("auth0|active"))
                .thenReturn(Optional.of(user("auth0|active", Role.SUPER_ADMIN, true)));

        AbstractAuthenticationToken token = new Auth0JwtAuthenticationConverter(loadUser).convert(jwt("auth0|active"));

        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SUPER_ADMIN");
    }

    @Test
    void deactivatedUser_getsNoAuthorities() {
        when(loadUser.loadByAuth0Sub("auth0|inactive"))
                .thenReturn(Optional.of(user("auth0|inactive", Role.FINANCE_STAFF, false)));

        AbstractAuthenticationToken token = new Auth0JwtAuthenticationConverter(loadUser).convert(jwt("auth0|inactive"));

        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void unprovisionedSub_getsNoAuthorities() {
        when(loadUser.loadByAuth0Sub("auth0|new")).thenReturn(Optional.empty());

        AbstractAuthenticationToken token = new Auth0JwtAuthenticationConverter(loadUser).convert(jwt("auth0|new"));

        assertThat(token.getAuthorities()).isEmpty();
    }
}
