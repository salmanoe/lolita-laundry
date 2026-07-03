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
    // While unprovisioned (204 → null), poll so a SUPER_ADMIN approval lands automatically — the
    // user drops off the "Menunggu Persetujuan" screen without having to hit "Periksa Lagi". Stops
    // the moment a role resolves (data is non-null), so a provisioned user never polls.
    refetchInterval: (query) => (query.state.data == null ? 15_000 : false),
    queryFn: async () => {
      const token = await getAccessTokenSilently()
      // 204 → apiFetch returns undefined; normalise to null so react-query treats it as resolved data.
      const me = await apiFetch<Me | undefined>('/api/me', { token })
      return me ?? null
    },
  })
}
