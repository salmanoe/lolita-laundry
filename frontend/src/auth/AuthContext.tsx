import { createContext, ReactNode, useContext, useMemo } from 'react'
import { Auth0Provider, useAuth0, User } from '@auth0/auth0-react'

// ── Auth state contract ───────────────────────────────────────────────────────

export interface AuthState {
  isAuthenticated: boolean
  isLoading: boolean
  user: User | undefined
  getAccessTokenSilently: () => Promise<string>
  loginWithRedirect: () => void
  logout: (opts?: { logoutParams?: { returnTo?: string } }) => void
}

const AuthContext = createContext<AuthState>(null!)

/** Use this hook everywhere instead of useAuth0() directly. */
export const useAuth = (): AuthState => useContext(AuthContext)

// ── Mock provider (VITE_AUTH_MOCK=true) ──────────────────────────────────────

const MOCK_STATE: AuthState = {
  isAuthenticated: true,
  isLoading: false,
  user: { name: 'Dev User (Mock)', email: 'dev@lolita.co.id', sub: 'mock|dev' },
  getAccessTokenSilently: async () => 'dev-mock-token',
  loginWithRedirect: () => {},
  logout: () => {},
}

function MockAuthProvider({ children }: { children: ReactNode }) {
  return <AuthContext.Provider value={MOCK_STATE}>{children}</AuthContext.Provider>
}

// ── Real Auth0 bridge ─────────────────────────────────────────────────────────
// Sits inside Auth0Provider, reads from useAuth0(), and feeds our AuthContext.

function Auth0Bridge({ children }: { children: ReactNode }) {
  const auth0 = useAuth0()

  const value: AuthState = useMemo(
    () => ({
      isAuthenticated: auth0.isAuthenticated,
      isLoading: auth0.isLoading,
      user: auth0.user,
      getAccessTokenSilently: () => auth0.getAccessTokenSilently(),
      loginWithRedirect: () => auth0.loginWithRedirect(),
      logout: (opts) => auth0.logout(opts),
    }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [auth0.isAuthenticated, auth0.isLoading, auth0.user],
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
