import { useEffect, useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import ItemFormModal from '../components/ItemFormModal'
import Pagination from '../components/Pagination'
import { indexById, useLookupList } from '../lib/lookups'
import type { Item, Page } from '../types/api'
// Item category was removed in V9 — items are grouped by the per-client department (Atur Harga).

const SIZE = 10

export default function ItemsPage() {
  const { getAccessTokenSilently } = useAuth()
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [form, setForm] = useState<{ open: boolean; item?: Item }>({ open: false })

  // Debounce the search term and reset to the first page whenever it changes.
  useEffect(() => {
    const t = setTimeout(() => {
      setDebouncedSearch(search.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(t)
  }, [search])

  const { data, isLoading, error } = useQuery({
    queryKey: ['items', { page, search: debouncedSearch }],
    queryFn: async () => {
      const token = await getAccessTokenSilently()
      const q = debouncedSearch ? `&search=${encodeURIComponent(debouncedSearch)}` : ''
      return apiFetch<Page<Item>>(`/api/items?page=${page}&size=${SIZE}&sort=name${q}`, { token })
    },
    placeholderData: keepPreviousData, // keep current rows visible while the next page loads
  })

  const unitsById = indexById(useLookupList('item-units').data)

  const items = data?.content ?? []

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-800">Item Laundry</h1>
        <button
          onClick={() => setForm({ open: true })}
          className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
        >
          + Tambah Item
        </button>
      </div>

      <input
        type="search"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        placeholder="Cari item..."
        className="mb-4 w-full max-w-sm rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
      />

      {isLoading ? (
        <div className="text-sm text-gray-400">Memuat daftar item...</div>
      ) : error ? (
        <div className="text-sm text-red-500">Gagal memuat daftar item.</div>
      ) : (
      <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
        <table className="min-w-full divide-y divide-gray-200 text-sm">
          <thead className="bg-gray-50">
            <tr>
              {['Nama', 'Satuan', 'Status', ''].map((h) => (
                <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {items.map((item) => (
              <tr key={item.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 font-medium text-gray-800">{item.name}</td>
                <td className="px-4 py-3 text-gray-500">{unitsById.get(item.unitId)?.displayName ?? '—'}</td>
                <td className="px-4 py-3">
                  <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                    item.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                  }`}>
                    {item.active ? 'Aktif' : 'Nonaktif'}
                  </span>
                </td>
                <td className="px-4 py-3 text-right">
                  <button
                    onClick={() => setForm({ open: true, item })}
                    className="text-sm font-medium text-brand-600 hover:text-brand-700"
                  >
                    Ubah
                  </button>
                </td>
              </tr>
            ))}
            {items.length === 0 && (
              <tr>
                <td colSpan={4} className="px-4 py-8 text-center text-sm text-gray-400">
                  {debouncedSearch
                    ? `Tidak ada item yang cocok dengan "${debouncedSearch}".`
                    : 'Belum ada item. Klik "Tambah Item" untuk menambahkan.'}
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
      )}

      <ItemFormModal
        open={form.open}
        item={form.item}
        onClose={() => setForm({ open: false })}
      />
    </div>
  )
}
