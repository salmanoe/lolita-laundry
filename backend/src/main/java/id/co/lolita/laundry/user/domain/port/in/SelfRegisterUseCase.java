package id.co.lolita.laundry.user.domain.port.in;

/**
 * First-login self-registration. Called by any authenticated caller whose Auth0 {@code sub} is
 * not yet provisioned as a {@link id.co.lolita.laundry.user.domain.User} — it records them in the
 * pending queue for a SUPER_ADMIN to approve. Idempotent: a no-op when a real user already exists,
 * and an upsert (by {@code sub}) for the pending row.
 */
public interface SelfRegisterUseCase {

    /**
     * Record the caller as awaiting approval. {@code auth0Sub} is the caller's already-verified
     * identity (read from the JWT, never the request body). {@code email}/{@code fullName} are
     * best-effort profile hints from the Auth0 ID token. No-op if the sub is already a real user.
     */
    void selfRegister(String auth0Sub, String email, String fullName);
}
