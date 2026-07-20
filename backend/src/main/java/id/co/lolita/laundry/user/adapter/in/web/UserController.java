package id.co.lolita.laundry.user.adapter.in.web;

import id.co.lolita.laundry.user.adapter.in.web.dto.MeResponse;
import id.co.lolita.laundry.user.adapter.in.web.dto.SelfRegisterRequest;
import id.co.lolita.laundry.user.domain.port.in.LoadUserByAuth0SubUseCase;
import id.co.lolita.laundry.user.domain.port.in.SelfRegisterUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
     * The current user's profile. Three outcomes:
     * <ul>
     *   <li>unauthenticated (dev) or Auth0 {@code sub} not yet provisioned → {@code 204} — the
     *       frontend treats that as "no special role" and renders the pending/self-register flow;</li>
     *   <li>provisioned and active → {@code 200} with the profile;</li>
     *   <li>provisioned but <b>deactivated</b> → {@code 403} with detail {@code "account_deactivated"}.</li>
     * </ul>
     *
     * <p><b>Revoke semantics.</b> The backend is a stateless JWT resource server, so a deactivated
     * user still holds a valid (non-expired) Auth0 access token. Every business endpoint is
     * {@code @PreAuthorize hasRole(...)} and {@link Auth0JwtAuthenticationConverter} grants a
     * deactivated user <i>no</i> authorities, so those already 403 on the next request. This method
     * (and {@code /me/register}) are the only {@code authenticated()}-only endpoints; returning 403
     * here closes that last gap and gives the SPA a machine-readable signal to show the deactivated
     * screen instead of the app shell.
     */
    @GetMapping("/me")
    ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.noContent().build();
        }
        var user = loadUser.loadByAuth0Sub(authentication.getName());
        if (user.isEmpty()) {
            return ResponseEntity.noContent().build();   // unprovisioned → pending/self-register
        }
        if (!user.get().isActive()) {
            // Return the ProblemDetail as the response body (like GlobalExceptionHandler) rather than
            // throwing ErrorResponseException — a thrown one only renders its body when
            // spring.mvc.problemdetails.enabled is on, which would leave the SPA's
            // detail === "account_deactivated" check with an empty body.
            var problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "account_deactivated");
            problem.setTitle("Account Deactivated");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
        }
        return ResponseEntity.ok(MeResponse.from(user.get()));
    }

    /**
     * First-login self-registration: an authenticated caller with no provisioned {@code users} row
     * queues themselves for SUPER_ADMIN approval. The {@code sub} is taken from the verified JWT
     * (never the body). Idempotent — a no-op once the caller is a real user (including a
     * <i>deactivated</i> one, so a deactivated account cannot re-queue itself for approval). Returns
     * 204 when there is no authenticated principal (dev permit-all / mock), where self-registration
     * is meaningless.
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