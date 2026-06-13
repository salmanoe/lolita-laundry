package id.co.lolita.laundry.user.adapter.in.web.dto;

import jakarta.validation.constraints.Size;

/**
 * First-login self-registration payload. The caller's {@code sub} is taken from the verified JWT,
 * never this body; only the best-effort profile hints from the Auth0 ID token are sent here.
 */
public record SelfRegisterRequest(
        @Size(max = 160) String email,
        @Size(max = 100) String fullName) {
}
