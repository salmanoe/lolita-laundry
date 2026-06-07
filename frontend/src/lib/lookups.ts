import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { Lookup } from '../types/api'

/** The managed reference-data collections. The string is also the API path segment. */
export type LookupKind = 'item-units' | 'client-types'

export const lookupKindLabel: Record<LookupKind, string> = {
  'item-units': 'Satuan Item',
  'client-types': 'Tipe Klien',
}

/**
 * Fetches the full list (active + inactive) of a lookup collection. One query per kind,
 * shared across the app via the [kind] query key — CRUD invalidates [kind] to refresh.
 * Components filter `.active` themselves for selection dropdowns.
 */
export function useLookupList(kind: LookupKind) {
  const { getAccessTokenSilently } = useAuth()
  return useQuery({
    queryKey: [kind],
    queryFn: async () => apiFetch<Lookup[]>(`/api/${kind}`, { token: await getAccessTokenSilently() }),
  })
}

/** Builds an id → Lookup map for resolving display names on list pages. */
export function indexById(list: Lookup[] | undefined): Map<number, Lookup> {
  return new Map((list ?? []).map((l) => [l.id, l]))
}