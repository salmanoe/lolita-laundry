import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import ClientFormModal from '../components/ClientFormModal'
import DepartmentFormModal from '../components/DepartmentFormModal'
import SetPriceModal from '../components/SetPriceModal'
import { billingModeLabel } from '../lib/labels'
import { indexById, useLookupList } from '../lib/lookups'
import type { Client, Department, Item, PriceListEntry } from '../types/api'

const rupiah = (n: number) =>
  new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)

export default function ClientDetailPage() {
  const { id } = useParams()
  const clientId = Number(id)
  const { getAccessTokenSilently } = useAuth()

  const [editClient, setEditClient] = useState(false)
  const [deptForm, setDeptForm] = useState<{ open: boolean; department?: Department }>({ open: false })
  const [priceForm, setPriceForm] = useState<{ open: boolean; presetItemId?: number }>({ open: false })

  const token = async () => getAccessTokenSilently()

  const clientQ = useQuery({
    queryKey: ['client', clientId],
    queryFn: async () => apiFetch<Client>(`/api/clients/${clientId}`, { token: await token() }),
  })
  const deptQ = useQuery({
    queryKey: ['departments', clientId],
    queryFn: async () => apiFetch<Department[]>(`/api/clients/${clientId}/departments`, { token: await token() }),
  })
  const priceQ = useQuery({
    queryKey: ['prices', clientId],
    queryFn: async () => apiFetch<PriceListEntry[]>(`/api/clients/${clientId}/prices`, { token: await token() }),
  })
  const itemsQ = useQuery({
    queryKey: ['items', 'options'],
    queryFn: async () => apiFetch<Item[]>('/api/items/options', { token: await token() }),
  })

  const typesById = indexById(useLookupList('client-types').data)
  const unitsById = indexById(useLookupList('item-units').data)

  if (clientQ.isLoading) return <div className="text-sm text-gray-400">Memuat data klien...</div>
  if (clientQ.error || !clientQ.data) return <div className="text-sm text-red-500">Gagal memuat data klien.</div>

  const client = clientQ.data
  const itemsById = new Map((itemsQ.data ?? []).map((i) => [i.id, i]))

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <Link to="/clients" className="text-sm text-gray-500 hover:text-gray-700">← Klien</Link>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-xl font-semibold text-gray-800">{client.name}</h1>
            <span className="rounded bg-gray-100 px-2 py-0.5 font-mono text-xs font-medium text-gray-600">
              {client.clientCode}
            </span>
            <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
              client.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
            }`}>
              {client.active ? 'Aktif' : 'Nonaktif'}
            </span>
          </div>
          <button
            onClick={() => setEditClient(true)}
            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Ubah Info
          </button>
        </div>
      </div>

      {/* Info card */}
      <dl className="grid grid-cols-2 gap-x-6 gap-y-4 rounded-lg border bg-white p-6 text-sm shadow-sm md:grid-cols-3">
        <Info label="Tipe" value={typesById.get(client.clientTypeId)?.displayName ?? '—'} />
        <Info label="Penagihan" value={billingModeLabel[client.billingMode]} />
        <Info label="Kontak" value={client.contactPerson ?? '—'} />
        <Info label="Telepon" value={client.phone ?? '—'} />
        <Info label="Alamat" value={client.address ?? '—'} />
        <Info label="Token Order" value={<span className="font-mono text-xs">{client.orderToken}</span>} />
      </dl>

      {/* Departments */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <div>
            <h2 className="text-base font-semibold text-gray-800">Departemen</h2>
            {client.billingMode !== 'PER_DEPARTMENT' && (
              <p className="text-xs text-gray-400">Hanya relevan untuk penagihan per departemen.</p>
            )}
          </div>
          <button
            onClick={() => setDeptForm({ open: true })}
            className="rounded-lg bg-brand-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-700"
          >
            + Tambah Departemen
          </button>
        </div>
        <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['Nama', 'Status', ''].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {deptQ.data?.map((dept) => (
                <tr key={dept.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-800">{dept.name}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                      dept.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                    }`}>
                      {dept.active ? 'Aktif' : 'Nonaktif'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => setDeptForm({ open: true, department: dept })}
                      className="text-sm font-medium text-brand-600 hover:text-brand-700"
                    >
                      Ubah
                    </button>
                  </td>
                </tr>
              ))}
              {deptQ.data?.length === 0 && (
                <tr><td colSpan={3} className="px-4 py-6 text-center text-sm text-gray-400">Belum ada departemen.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      {/* Price list */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-base font-semibold text-gray-800">Daftar Harga</h2>
          <button
            onClick={() => setPriceForm({ open: true })}
            className="rounded-lg bg-brand-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-700"
          >
            + Atur Harga
          </button>
        </div>
        <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['Item', 'Satuan', 'Harga', 'Berlaku Mulai', ''].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {priceQ.data?.map((price) => {
                const item = itemsById.get(price.itemId)
                return (
                  <tr key={price.itemId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-gray-800">{item?.name ?? `#${price.itemId}`}</td>
                    <td className="px-4 py-3 text-gray-500">{item ? (unitsById.get(item.unitId)?.displayName ?? '—') : '—'}</td>
                    <td className="px-4 py-3 font-medium text-gray-700">{rupiah(price.pricePerUnit)}</td>
                    <td className="px-4 py-3 text-gray-500">{price.effectiveDate}</td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => setPriceForm({ open: true, presetItemId: price.itemId })}
                        className="text-sm font-medium text-brand-600 hover:text-brand-700"
                      >
                        Ubah Harga
                      </button>
                    </td>
                  </tr>
                )
              })}
              {priceQ.data?.length === 0 && (
                <tr><td colSpan={5} className="px-4 py-6 text-center text-sm text-gray-400">Belum ada harga.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      <ClientFormModal open={editClient} client={client} onClose={() => setEditClient(false)} />
      <DepartmentFormModal
        open={deptForm.open}
        clientId={clientId}
        department={deptForm.department}
        onClose={() => setDeptForm({ open: false })}
      />
      <SetPriceModal
        open={priceForm.open}
        clientId={clientId}
        presetItemId={priceForm.presetItemId}
        onClose={() => setPriceForm({ open: false })}
      />
    </div>
  )
}

function Info({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs font-medium text-gray-400">{label}</dt>
      <dd className="mt-0.5 text-gray-800">{value}</dd>
    </div>
  )
}
