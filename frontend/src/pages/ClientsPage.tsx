import { useState } from 'react'
import { Link } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import ClientFormModal from '../components/ClientFormModal'
import Pagination from '../components/Pagination'
import { billingModeLabel } from '../lib/labels'
import { indexById, useLookupList } from '../lib/lookups'
import type { Client, Page } from '../types/api'

const SIZE = 10

export default function ClientsPage() {
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [form, setForm] = useState<{ open: boolean; client?: Client }>({ open: false })

  const { data, isLoading, error } = useQuery({
    queryKey: ['clients', { page }],
    queryFn: async () => {
      const token = await getAccessTokenSilently()
      return apiFetch<Page<Client>>(`/api/clients?page=${page}&size=${SIZE}&sort=name`, { token })
    },
    placeholderData: keepPreviousData,
  })

  const rotateToken = useMutation({
    mutationFn: async (id: number) => {
      const token = await getAccessTokenSilently()
      return apiFetch(`/api/clients/${id}/rotate-token`, { method: 'POST', token })
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clients'] }),
  })

  const typesById = indexById(useLookupList('client-types').data)

  if (isLoading) return <div className="text-sm text-gray-400">Memuat data klien...</div>
  if (error)    return <div className="text-sm text-red-500">Gagal memuat data klien.</div>

  const clients = data?.content ?? []

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-800">Klien</h1>
        <button
          onClick={() => setForm({ open: true })}
          className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
        >
          + Tambah Klien
        </button>
      </div>

      <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
        <table className="min-w-full divide-y divide-gray-200 text-sm">
          <thead className="bg-gray-50">
            <tr>
              {['Kode', 'Nama', 'Tipe', 'Penagihan', 'Kontak', 'Status', ''].map((h) => (
                <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {clients.map((client) => (
              <tr key={client.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 font-mono font-medium text-gray-700">{client.clientCode}</td>
                <td className="px-4 py-3">
                  <Link to={`/clients/${client.id}`} className="font-medium text-brand-700 hover:underline">
                    {client.name}
                  </Link>
                </td>
                <td className="px-4 py-3 text-gray-500">{typesById.get(client.clientTypeId)?.displayName ?? '—'}</td>
                <td className="px-4 py-3 text-gray-500">{billingModeLabel[client.billingMode]}</td>
                <td className="px-4 py-3 text-gray-500">{client.contactPerson ?? '—'}</td>
                <td className="px-4 py-3">
                  <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                    client.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                  }`}>
                    {client.active ? 'Aktif' : 'Nonaktif'}
                  </span>
                </td>
                <td className="px-4 py-3 text-right whitespace-nowrap">
                  <button
                    onClick={() => setForm({ open: true, client })}
                    className="text-sm font-medium text-brand-600 hover:text-brand-700"
                  >
                    Ubah
                  </button>
                  <button
                    onClick={() => {
                      if (confirm(`Putar ulang token order untuk ${client.name}? Tautan order lama akan berhenti berfungsi.`)) {
                        rotateToken.mutate(client.id)
                      }
                    }}
                    disabled={rotateToken.isPending}
                    className="ml-4 text-sm font-medium text-gray-500 hover:text-gray-700 disabled:opacity-50"
                  >
                    Putar Token
                  </button>
                </td>
              </tr>
            ))}
            {clients.length === 0 && (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-sm text-gray-400">
                  Belum ada klien. Klik "Tambah Klien" untuk menambahkan.
                </td>
              </tr>
            )}
          </tbody>
        </table>
        <Pagination
          page={data?.page ?? 0}
          totalPages={data?.totalPages ?? 0}
          totalElements={data?.totalElements ?? 0}
          onPage={setPage}
        />
      </div>

      <ClientFormModal
        open={form.open}
        client={form.client}
        onClose={() => setForm({ open: false })}
      />
    </div>
  )
}
