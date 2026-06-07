import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import LookupFormModal from '../components/LookupFormModal'
import { lookupKindLabel, useLookupList, type LookupKind } from '../lib/lookups'
import type { Lookup } from '../types/api'

const KINDS: LookupKind[] = ['item-units', 'client-types']

export default function MasterDataPage() {
  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-xl font-semibold text-gray-800">Master Data</h1>
        <p className="text-sm text-gray-500">Kelola satuan item dan tipe klien.</p>
      </div>
      {KINDS.map((kind) => (
        <LookupSection key={kind} kind={kind} />
      ))}
    </div>
  )
}

function LookupSection({ kind }: { kind: LookupKind }) {
  const title = lookupKindLabel[kind]
  const { data, isLoading, error } = useLookupList(kind)
  const [form, setForm] = useState<{ open: boolean; entry?: Lookup }>({ open: false })
  const [q, setQ] = useState('')
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()

  // Reorder by swapping the two rows' sortOrder (data arrives sorted by sortOrder).
  const reorder = useMutation({
    mutationFn: async ({ a, b }: { a: Lookup; b: Lookup }) => {
      const token = await getAccessTokenSilently()
      await apiFetch(`/api/${kind}/${a.id}`, {
        method: 'PUT', token,
        body: JSON.stringify({ displayName: a.displayName, sortOrder: b.sortOrder, active: a.active }),
      })
      await apiFetch(`/api/${kind}/${b.id}`, {
        method: 'PUT', token,
        body: JSON.stringify({ displayName: b.displayName, sortOrder: a.sortOrder, active: b.active }),
      })
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: [kind] }),
  })

  const filtering = q.trim() !== ''
  const term = q.trim().toLowerCase()
  const rows = (data ?? []).filter(
    (r) => !filtering || r.code.toLowerCase().includes(term) || r.displayName.toLowerCase().includes(term),
  )

  return (
    <section>
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="text-base font-semibold text-gray-800">{title}</h2>
        <div className="flex items-center gap-3">
          {(data?.length ?? 0) > 8 && (
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder={`Cari ${title.toLowerCase()}...`}
              className="w-48 rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            />
          )}
          <button
            onClick={() => setForm({ open: true })}
            className="whitespace-nowrap rounded-lg bg-brand-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-700"
          >
            + Tambah
          </button>
        </div>
      </div>

      {isLoading && <div className="text-sm text-gray-400">Memuat...</div>}
      {error && <div className="text-sm text-red-500">Gagal memuat {title}.</div>}

      {data && (
        <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['Urutan', 'Kode', 'Nama', 'Status', ''].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {rows.map((row, i) => (
                <tr key={row.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <div className="flex gap-1 text-gray-400">
                      <button
                        disabled={filtering || i === 0 || reorder.isPending}
                        onClick={() => reorder.mutate({ a: row, b: rows[i - 1] })}
                        className="rounded px-1 hover:bg-gray-100 hover:text-gray-700 disabled:opacity-30"
                        aria-label="Naikkan"
                        title={filtering ? 'Hapus pencarian untuk mengurutkan' : 'Naikkan'}
                      >↑</button>
                      <button
                        disabled={filtering || i === rows.length - 1 || reorder.isPending}
                        onClick={() => reorder.mutate({ a: row, b: rows[i + 1] })}
                        className="rounded px-1 hover:bg-gray-100 hover:text-gray-700 disabled:opacity-30"
                        aria-label="Turunkan"
                        title={filtering ? 'Hapus pencarian untuk mengurutkan' : 'Turunkan'}
                      >↓</button>
                    </div>
                  </td>
                  <td className="px-4 py-3 font-mono font-medium text-gray-700">{row.code}</td>
                  <td className="px-4 py-3 text-gray-800">{row.displayName}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                      row.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                    }`}>
                      {row.active ? 'Aktif' : 'Nonaktif'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => setForm({ open: true, entry: row })}
                      className="text-sm font-medium text-brand-600 hover:text-brand-700"
                    >
                      Ubah
                    </button>
                  </td>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-6 text-center text-sm text-gray-400">
                    {filtering ? 'Tidak ada hasil.' : 'Belum ada data.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      <LookupFormModal
        open={form.open}
        kind={kind}
        title={title}
        entry={form.entry}
        onClose={() => setForm({ open: false })}
      />
    </section>
  )
}
