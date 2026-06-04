package id.co.lolita.laundry.order.adapter.in.web;

import id.co.lolita.laundry.user.domain.port.in.UserDirectoryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Resolves the acting Lolita user's database id from the current authentication so order
 * actions can be attributed. Returns {@code null} when unauthenticated (e.g. the dev
 * profile permits all requests) or when the Auth0 {@code sub} is not yet provisioned —
 * the {@code created_by}/{@code changed_by} columns are nullable.
 */
@Component
@RequiredArgsConstructor
class CurrentUserResolver {

    private final UserDirectoryQuery userDirectory;

    Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return userDirectory.idForAuth0Sub(authentication.getName()).orElse(null);
    }
}