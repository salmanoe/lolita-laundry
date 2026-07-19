import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import { useAuth } from './AuthContext'
import type { Me } from '../types/api'

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
      const token = await getAccessTokenSilently()
      // 204 → apiFetch returns undefined; normalise to null so react-query treats it as resolved data.
      const me = await apiFetch<Me | undefined>('/api/me', { token })
      return me ?? null
    },
  })
}
