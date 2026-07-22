import { createContext, ReactNode, useContext, useMemo } from 'react'
import { Auth0Provider, useAuth0, User } from '@auth0/auth0-react'
import { purgeAuthCache } from './recovery'

// ── Auth state contract ───────────────────────────────────────────────────────

export interface AuthState {
  isAuthenticated: boolean
  isLoading: boolean
  /**
   * Set when the SDK failed to establish a session (e.g. `checkSession()` rejected because the
   * stored refresh token was revoked by Refresh Token Rotation reuse detection, or iOS/Safari ITP
   * evicted part of the localStorage cache). Callers MUST render an escape hatch when this is set —
   * without one the app sits on a spinner forever and only a manual "clear site data" recovers it.
   */
  error: Error | undefined
  user: User | undefined
  getAccessTokenSilently: () => Promise<string>
  loginWithRedirect: () => Promise<void>
  logout: (opts?: { logoutParams?: { returnTo?: string; federated?: boolean } }) => void
}

const AuthContext = createContext<AuthState>(null!)

/** Use this hook everywhere instead of useAuth0() directly. */
export const useAuth = (): AuthState => useContext(AuthContext)

// ── Mock provider (VITE_AUTH_MOCK=true) ──────────────────────────────────────

const MOCK_STATE: AuthState = {
  isAuthenticated: true,
  isLoading: false,
  error: undefined,
  user: { name: 'Dev User (Mock)', email: 'dev@lolita.co.id', sub: 'mock|dev' },
  getAccessTokenSilently: async () => 'dev-mock-token',
  loginWithRedirect: async () => {},
  logout: () => {},
}

function MockAuthProvider({ children }: { children: ReactNode }) {
  return <AuthContext.Provider value={MOCK_STATE}>{children}</AuthContext.Provider>
}

// ── Real Auth0 bridge ─────────────────────────────────────────────────────────
// Sits inside Auth0Provider, reads from useAuth0(), and feeds our AuthContext.

/**
 * Auth0 error codes that mean "this session is gone, only a fresh login fixes it" — as opposed to a
 * transient network blip, which must NOT bounce the user out of the app.
 */
const UNRECOVERABLE = new Set([
  'login_required',
  'consent_required',
  'invalid_grant',
  'missing_refresh_token',
])

function Auth0Bridge({ children }: { children: ReactNode }) {
  const auth0 = useAuth0()

  const value: AuthState = useMemo(
    () => ({
      isAuthenticated: auth0.isAuthenticated,
      isLoading: auth0.isLoading,
      error: auth0.error,
      user: auth0.user,
      // Mid-session recovery. Every queryFn in the app calls this; if the refresh token was revoked
      // while the user was working (Refresh Token Rotation reuse detection kills the whole family),
      // each call throws and the user is left staring at broken screens with a valid-looking UI and
      // no prompt to log in again. Catch that one class of failure, purge the dead cache and send
      // them through login — instead of surfacing "invalid_grant" as a data-loading error.
      getAccessTokenSilently: async () => {
        try {
          return await auth0.getAccessTokenSilently()
        } catch (e) {
          const code = (e as { error?: string })?.error
          if (code && UNRECOVERABLE.has(code)) {
            purgeAuthCache()
            void auth0.loginWithRedirect()
          }
          throw e
        }
      },
      // Logout is deliberately NOT federated (that would end the user's entire Google browser
      // session, not just Lolita). Ending the Auth0 tenant session alone means Google would
      // silently re-select the still-signed-in account on the next login, so force the account
      // chooser here — that is how a shared device switches users.
      loginWithRedirect: () =>
        auth0.loginWithRedirect({ authorizationParams: { prompt: 'select_account' } }),
      logout: (opts) => auth0.logout(opts),
    }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [auth0.isAuthenticated, auth0.isLoading, auth0.error, auth0.user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// ── Unified AuthProvider ──────────────────────────────────────────────────────

interface AuthProviderProps {
  children: ReactNode
}

/**
 * Top-level auth provider.
 *
 * Set {@code VITE_AUTH_MOCK=true} in {@code .env.local} to bypass Auth0 entirely.
 * Useful during development before an Auth0 account is configured.
 * In mock mode the backend must run with the dev profile (which permits all requests).
 */
export function AuthProvider({ children }: AuthProviderProps) {
  if (import.meta.env.VITE_AUTH_MOCK === 'true') {
    return <MockAuthProvider>{children}</MockAuthProvider>
  }

  const domain   = import.meta.env.VITE_AUTH0_DOMAIN
  const clientId = import.meta.env.VITE_AUTH0_CLIENT_ID
  const audience = import.meta.env.VITE_AUTH0_AUDIENCE

  return (
    <Auth0Provider
      domain={domain}
      clientId={clientId}
      authorizationParams={{
        redirect_uri: window.location.origin,
        audience,
        scope: 'openid profile email',
        // Render the Auth0 Universal Login in Bahasa Indonesia (the app's language).
        // Requires Indonesian to be enabled on the Auth0 tenant (Settings → Languages).
        ui_locales: 'id',
      }}
      // Persist the session across reloads (default 'memory' loses it on refresh,
      // which can re-trigger the login redirect) and use refresh tokens for renewal.
      cacheLocation="localstorage"
      useRefreshTokens={true}
    >
      <Auth0Bridge>{children}</Auth0Bridge>
    </Auth0Provider>
  )
}
