package id.co.lolita.laundry.user.adapter.in.web;

import id.co.lolita.laundry.user.adapter.in.web.dto.MeResponse;
import id.co.lolita.laundry.user.adapter.in.web.dto.SelfRegisterRequest;
import id.co.lolita.laundry.user.domain.port.in.LoadUserByAuth0SubUseCase;
import id.co.lolita.laundry.user.domain.port.in.SelfRegisterUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated user-directory endpoints.
 *
 * <p>{@code GET /api/me} is open to any authenticated user (every role calls it for
 * role-aware routing). Method-level {@code @PreAuthorize} is enforced in prod via
 * {@code SecurityConfig}; the dev profile permits all requests, matching the rest of the app.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
class UserController {

    private final LoadUserByAuth0SubUseCase loadUser;
    private final SelfRegisterUseCase selfRegister;

    /**
     * The current user's profile. Returns 204 when the caller is unauthenticated (dev) or
     * their Auth0 {@code sub} is not yet provisioned — the frontend treats that as "no
     * special role" and renders the staff/admin app.
     */
    @GetMapping("/me")
    ResponseEntity<MeResponse> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.noContent().build();
        }
        return loadUser.loadByAuth0Sub(authentication.getName())
                .map(MeResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * First-login self-registration: an authenticated caller with no provisioned {@code users} row
     * queues themselves for SUPER_ADMIN approval. The {@code sub} is taken from the verified JWT
     * (never the body). Idempotent — a no-op once the caller is a real user. Returns 204 when there
     * is no authenticated principal (dev permit-all / mock), where self-registration is meaningless.
     */
    @PostMapping("/me/register")
    ResponseEntity<Void> register(Authentication authentication, @Valid @RequestBody SelfRegisterRequest request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.noContent().build();
        }
        selfRegister.selfRegister(authentication.getName(), request.email(), request.fullName());
        return ResponseEntity.noContent().build();
    }
}