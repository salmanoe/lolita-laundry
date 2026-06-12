package id.co.lolita.laundry.user.adapter.in.security;

import id.co.lolita.laundry.user.domain.User;
import id.co.lolita.laundry.user.domain.port.in.LoadUserByAuth0SubUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts a validated Auth0 JWT into a Spring Security {@link AbstractAuthenticationToken}.
 *
 * <p>Loads the user's role from our {@code users} table via the Auth0 {@code sub} claim,
 * so that role-based access control ({@code @PreAuthorize}) works with our business roles
 * (SUPER_ADMIN / FINANCE_STAFF / DAILY_STAFF) rather than Auth0-defined roles.
 *
 * <p>If the {@code sub} is not in our table, authentication succeeds but the token carries
 * no granted authorities — effectively read-only until a SUPER_ADMIN provisions the user.
 */
@Component
@RequiredArgsConstructor
class Auth0JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final LoadUserByAuth0SubUseCase loadUser;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String auth0Sub = jwt.getSubject();

        var authorities = loadUser.loadByAuth0Sub(auth0Sub)
                .filter(User::isActive)
                .map(user -> List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .orElse(List.of());

        return new UsernamePasswordAuthenticationToken(auth0Sub, jwt, authorities);
    }
}
