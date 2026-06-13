import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { roleLabel } from '../lib/labels'
import type { PendingUser, Role } from '../types/api'

const ROLES: Role[] = ['SUPER_ADMIN', 'FINANCE_STAFF', 'DAILY_STAFF']

/**
 * SUPER_ADMIN review queue for self-registered logins ("Permintaan Akses"). Each request is a user
 * who logged in via Auth0 (Google or email+password) but has no role yet. Approving creates their
 * `users` row with the chosen role; rejecting discards the request. Hidden entirely when empty.
 */
export default function PendingAccessRequests() {
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const [roles, setRoles] = useState<Record<number, Role>>({})

  const { data } = useQuery({
    queryKey: ['users', 'pending'],
    queryFn: async () => {
      const token = await getAccessTokenSilently()
      return apiFetch<PendingUser[]>('/api/users/pending', { token })
    },
  })

  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['users', 'pending'] })
    qc.invalidateQueries({ queryKey: ['users'] })
  }

  const approve = useMutation({
    mutationFn: async ({ id, role }: { id: number; role: Role }) => {
      const token = await getAccessTokenSilently()
      return apiFetch(`/api/users/pending/${id}/approve`, {
        method: 'POST',
        token,
        body: JSON.stringify({ role }),
      })
    },
    onSuccess: refresh,
  })

  const reject = useMutation({
    mutationFn: async (id: number) => {
      const token = await getAccessTokenSilently()
      return apiFetch(`/api/users/pending/${id}`, { method: 'DELETE', token })
    },
    onSuccess: refresh,
  })

  if (!data || data.length === 0) return null

  const pending = approve.isPending || reject.isPending
  const err = approve.error ?? reject.error

  return (
    <section className="rounded-lg border border-amber-200 bg-amber-50/60 p-4 shadow-sm">
      <h2 className="text-sm font-semibold text-amber-800">
        Permintaan Akses <span className="font-normal text-amber-600">({data.length})</span>
      </h2>
      <p className="mb-3 text-xs text-amber-700/80">
        Pengguna yang sudah masuk lewat Auth0 namun belum memiliki peran. Setujui dengan memilih peran, atau tolak.
      </p>

      {err && (
        <div className="mb-3 text-sm text-red-600">
          {err instanceof ApiError ? err.detail : 'Gagal memproses permintaan.'}
        </div>
      )}

      <div className="space-y-2">
        {data.map((p) => {
          const role = roles[p.id] ?? 'DAILY_STAFF'
          return (
            <div
              key={p.id}
              className="flex flex-col gap-2 rounded-md border border-amber-200 bg-white px-3 py-2.5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0">
                <div className="truncate text-sm font-medium text-gray-800">
                  {p.fullName ?? p.email ?? p.auth0Sub}
                </div>
                <div className="truncate text-xs text-gray-500">
                  {p.email ?? p.auth0Sub}
                  <span className="ml-2 text-gray-400">
                    {new Date(p.requestedAt).toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })}
                  </span>
                </div>
              </div>
              <div className="flex shrink-0 items-center gap-2">
                <select
                  value={role}
                  onChange={(e) => setRoles((r) => ({ ...r, [p.id]: e.target.value as Role }))}
                  className="rounded-md border border-gray-300 px-2 py-1.5 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
                >
                  {ROLES.map((r) => (
                    <option key={r} value={r}>
                      {roleLabel[r]}
                    </option>
                  ))}
                </select>
                <button
                  onClick={() => approve.mutate({ id: p.id, role })}
                  disabled={pending}
                  className="rounded-md bg-brand-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-40"
                >
                  Setujui
                </button>
                <button
                  onClick={() => reject.mutate(p.id)}
                  disabled={pending}
                  className="rounded-md px-2 py-1.5 text-sm font-medium text-red-600 hover:text-red-700 disabled:opacity-40"
                >
                  Tolak
                </button>
              </div>
            </div>
          )
        })}
      </div>
    </section>
  )
}
