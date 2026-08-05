import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import { useAuth } from './AuthContext'
import type { Me, Role } from '../types/api'

/**
 * Mock-mode role (dev only).
 *
 * In mock auth there is no JWT, so the dev-profile backend has no `Authentication` and
 * `GET /api/me` answers 204 — the caller resolves to *no* role. Most screens fail open on an
 * unresolved role, but the ones that gate on an explicit role (the company-profile and
 * bank-account editors, the dashboard's role branch, the Pengguna nav entry) then behave as if
 * you were the *least* privileged user, which makes the admin UI impossible to review locally.
 *
 * Setting `VITE_AUTH_MOCK_ROLE` in `.env.local` pins the role instead. Defaults to SUPER_ADMIN so
 * a fresh mock session sees the whole app; set it to FINANCE_STAFF or DAILY_STAFF to review those
 * shells. Gated on `VITE_AUTH_MOCK`, which is dev-only and gitignored — a production build has it
 * unset, so this branch is dead code there, exactly like MockAuthProvider.
 */
const MOCK_AUTH = import.meta.env.VITE_AUTH_MOCK === 'true'
const MOCK_ME: Me = {
  id: 0,
  fullName: 'Dev User (Mock)',
  role: ((import.meta.env.VITE_AUTH_MOCK_ROLE as Role | undefined) ?? 'SUPER_ADMIN'),
}

/**
 * Fetches the current Lolita user (id, name, role) once and caches it. Returns `undefined`
 * for the data when the caller is unauthenticated or unprovisioned (backend replies 204) —
 * callers treat that as "no special role" and render the staff/admin app. Used for
 * role-aware routing: a DRIVER is sent to the delivery screen and kept out of admin routes.
 */
export function useMe() {
  const { getAccessTokenSilently, isAuthenticated } = useAuth()
  return useQuery({
    queryKey: ['me'],
    enabled: isAuthenticated,
    staleTime: 5 * 60 * 1000,
    // A failed /api/me should not spin: cap retries so an outage (e.g. DB down) doesn't
    // fan out into three attempts per interval tick.
    retry: 1,
    // While unprovisioned (204 → null), poll so a SUPER_ADMIN approval lands automatically — the
    // user drops off the "Menunggu Persetujuan" screen without having to hit "Periksa Lagi". Stops
    // the moment a role resolves (data is non-null), so a provisioned user never polls.
    //
    // Poll gently: 60s base (was 15s). A stray tab left on the approval screen used to hit the DB
    // every 15s around the clock, keeping Neon's compute awake 24/7 and burning the free-tier
    // compute quota. On repeated failures back off (60s → 2m → cap 5m) so a backend/DB outage
    // isn't hammered by every open tab.
    refetchInterval: (query) => {
      if (query.state.data != null) return false // role resolved → stop polling
      const failures = query.state.fetchFailureCount
      if (failures > 0) return Math.min(60_000 * 2 ** failures, 5 * 60_000)
      return 60_000
    },
    queryFn: async () => {
      // Dev mock auth: there is no principal for the backend to resolve, so answer locally with
      // the pinned role rather than letting /api/me's 204 strip every role-gated affordance.
      if (MOCK_AUTH) return MOCK_ME
      const token = await getAccessTokenSilently()
      // 204 → apiFetch returns undefined; normalise to null so react-query treats it as resolved data.
      const me = await apiFetch<Me | undefined>('/api/me', { token })
      return me ?? null
    },
  })
}
