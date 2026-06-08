import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useMe } from '../auth/useMe'
import UserFormModal from '../components/UserFormModal'
import { roleBadge, roleLabel } from '../lib/labels'
import type { User } from '../types/api'

export default function UsersPage() {
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const meQ = useMe()
  const [form, setForm] = useState<{ open: boolean; entry?: User }>({ open: false })
  const [q, setQ] = useState('')

  const { data, isLoading, error } = useQuery({
    queryKey: ['users'],
    queryFn: async () => {
      const token = await getAccessTokenSilently()
      return apiFetch<User[]>('/api/users', { token })
    },
  })

  const toggle = useMutation({
    mutationFn: async (u: User) => {
      const token = await getAccessTokenSilently()
      return apiFetch(`/api/users/${u.id}/status`, {
        method: 'PATCH',
        token,
        body: JSON.stringify({ active: !u.active }),
      })
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
  })

  const filtering = q.trim() !== ''
  const term = q.trim().toLowerCase()
  const rows = (data ?? []).filter(
    (u) =>
      !filtering ||
      u.fullName.toLowerCase().includes(term) ||
      u.auth0Sub.toLowerCase().includes(term) ||
      roleLabel[u.role].toLowerCase().includes(term),
  )

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-gray-800">Pengguna</h1>
          <p className="text-sm text-gray-500">
            Kelola staf dan pengantar yang dapat masuk ke sistem. Buat akun di Auth0 lebih dulu, lalu daftarkan di sini.
          </p>
        </div>
        <button
          onClick={() => setForm({ open: true })}
          className="whitespace-nowrap rounded-lg bg-brand-600 px-3 py-2 text-sm font-medium text-white hover:bg-brand-700"
        >
          + Tambah
        </button>
      </div>

      {(data?.length ?? 0) > 8 && (
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Cari nama, sub, atau peran..."
          className="w-full max-w-xs rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
        />
      )}

      {isLoading && <div className="text-sm text-gray-400">Memuat...</div>}
      {error && (
        <div className="text-sm text-red-500">
          {error instanceof ApiError ? error.detail : 'Gagal memuat pengguna.'}
        </div>
      )}

      {toggle.isError && (
        <div className="text-sm text-red-600">
          {toggle.error instanceof ApiError ? toggle.error.detail : 'Gagal mengubah status.'}
        </div>
      )}

      {data && (
        <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['Nama', 'Auth0 Sub', 'Peran', 'Status', ''].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {rows.map((u) => {
                const isSelf = meQ.data?.id === u.id
                return (
                  <tr key={u.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-medium text-gray-800">
                      {u.fullName}
                      {isSelf && <span className="ml-2 text-xs font-normal text-gray-400">(Anda)</span>}
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-gray-500">{u.auth0Sub}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${roleBadge[u.role]}`}>
                        {roleLabel[u.role]}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                        u.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                      }`}>
                        {u.active ? 'Aktif' : 'Nonaktif'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-3">
                        <button
                          onClick={() => setForm({ open: true, entry: u })}
                          className="text-sm font-medium text-brand-600 hover:text-brand-700"
                        >
                          Ubah
                        </button>
                        <button
                          onClick={() => toggle.mutate(u)}
                          disabled={toggle.isPending}
                          className={`text-sm font-medium disabled:opacity-40 ${
                            u.active ? 'text-red-600 hover:text-red-700' : 'text-green-600 hover:text-green-700'
                          }`}
                        >
                          {u.active ? 'Nonaktifkan' : 'Aktifkan'}
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
              {rows.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-6 text-center text-sm text-gray-400">
                    {filtering ? 'Tidak ada hasil.' : 'Belum ada pengguna.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      <UserFormModal open={form.open} entry={form.entry} onClose={() => setForm({ open: false })} />
    </div>
  )
}