import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useMe } from '../auth/useMe'
import ClientFormModal from '../components/ClientFormModal'
import DepartmentFormModal from '../components/DepartmentFormModal'
import SetPriceModal from '../components/SetPriceModal'
import { billingModeLabel, isoDateLabel } from '../lib/labels'
import { indexById, useLookupList } from '../lib/lookups'
import { bankAccountLabel, useBankAccountOptions } from '../lib/bankAccounts'
import { previousPeriodStart } from '../lib/billingCycle'
import type { Client, Department, Item, PriceListEntry } from '../types/api'

const rupiah = (n: number) =>
  new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)

export default function ClientDetailPage() {
  const { id } = useParams()
  const clientId = Number(id)
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const isSuperAdmin = useMe().data?.role === 'SUPER_ADMIN'

  const [editClient, setEditClient] = useState(false)
  const [deptForm, setDeptForm] = useState<{ open: boolean; department?: Department }>({ open: false })
  const [priceForm, setPriceForm] = useState<{ open: boolean; presetItemId?: number; presetDepartmentId?: number; presetPrice?: number; presetEffectiveDate?: string }>({ open: false })
  const [copied, setCopied] = useState(false)
  const [priceSearch, setPriceSearch] = useState('')
  // null = follow the client's billing cycle (see resyncFrom below); a string is the user's
  // explicit override. Deliberately NOT seeded from the cycle at mount: the client is still
  // loading then, so a seeded default would freeze at whatever the cycle was on first render and
  // never follow a later change to it.
  const [resyncFromOverride, setResyncFromOverride] = useState<string | null>(null)

  const token = async () => getAccessTokenSilently()

  const clientQ = useQuery({
    queryKey: ['client', clientId],
    queryFn: async () => apiFetch<Client>(`/api/clients/${clientId}`, { token: await token() }),
  })

  // The default re-sync window is the start of the client's PREVIOUS billing period, so it always
  // lands on a period boundary. Anchoring it to the 1st of last month would start a cut-off
  // client mid-period and leave that period's earlier orders un-re-homed.
  const cycleDay = clientQ.data?.billingCycleDay ?? null
  const defaultResyncFrom = previousPeriodStart(new Date(), cycleDay)
  const resyncFrom = resyncFromOverride ?? defaultResyncFrom

  // Re-runs the order → billing sync for this client's recent orders (SUPER_ADMIN).
  const resync = useMutation({
    mutationFn: async () =>
      apiFetch<{ resyncedOrders: number }>(
        `/api/billing/resync/${clientId}?from=${resyncFrom}`,
        { method: 'POST', token: await token() },
      ),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ['billings'] })
      alert(`Sinkronisasi selesai: ${r.resyncedOrders} order diproses ulang.`)
    },
    onError: (e) => alert(e instanceof ApiError ? e.detail : 'Gagal menyinkronkan tagihan.'),
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

  const deleteItem = useMutation({
    mutationFn: async (itemId: number) =>
      apiFetch(`/api/clients/${clientId}/prices/${itemId}`, { method: 'DELETE', token: await token() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['prices', clientId] }),
    onError: (e) =>
      window.alert(e instanceof ApiError ? e.detail : 'Gagal menghapus item.'),
  })

  const typesById = indexById(useLookupList('client-types').data)
  const unitsById = indexById(useLookupList('item-units').data)
  const bankAccountsQ = useBankAccountOptions()

  if (clientQ.isLoading) return <div className="text-sm text-gray-400">Memuat data klien...</div>
  if (clientQ.error || !clientQ.data) return <div className="text-sm text-red-500">Gagal memuat data klien.</div>

  const client = clientQ.data
  const perDepartment = client.billingMode === 'PER_DEPARTMENT'
  const itemsById = new Map((itemsQ.data ?? []).map((i) => [i.id, i]))
  const deptById = new Map((deptQ.data ?? []).map((d) => [d.id, d]))
  const activeDepartments = (deptQ.data ?? []).filter((d) => d.active)

  const q = priceSearch.trim().toLowerCase()
  const filteredPrices = q
    ? (priceQ.data ?? []).filter((p) => (itemsById.get(p.itemId)?.name ?? '').toLowerCase().includes(q))
    : (priceQ.data ?? [])

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
          <div className="flex gap-2">
            <button
              onClick={async () => {
                const link = `${window.location.origin}/order/${client.orderToken}`
                await navigator.clipboard.writeText(link)
                setCopied(true)
                setTimeout(() => setCopied(false), 2000)
              }}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              {copied ? '✓ Tersalin' : 'Salin Tautan Order'}
            </button>
            <button
              onClick={() => setEditClient(true)}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Ubah Info
            </button>
          </div>
        </div>
      </div>

      {/* Info card */}
      <dl className="grid grid-cols-2 gap-x-6 gap-y-4 rounded-lg border bg-white p-6 text-sm shadow-sm md:grid-cols-3">
        <Info label="Tipe" value={typesById.get(client.clientTypeId)?.displayName ?? '—'} />
        <Info label="Penagihan" value={billingModeLabel[client.billingMode]} />
        <Info label="Kontak" value={client.contactPerson ?? '—'} />
        <Info label="Telepon" value={client.phone ?? '—'} />
        <Info label="Alamat" value={client.address ?? '—'} />
        <Info
          label="Rekening Transfer"
          value={
            <span className={client.bankAccountId == null ? 'text-gray-400' : undefined}>
              {bankAccountLabel(client.bankAccountId, bankAccountsQ.data)}
            </span>
          }
        />
        <Info
          label="Siklus Penagihan"
          value={
            client.billingCycleDay == null
              ? <span className="text-gray-400">Bulan kalender</span>
              : `Tutup tgl ${client.billingCycleDay} (tgl ${client.billingCycleDay + 1} – tgl ${client.billingCycleDay})`
          }
        />
        <Info label="Token Order" value={<span className="font-mono text-xs">{client.orderToken}</span>} />
      </dl>

      {/* Billing re-sync — SUPER_ADMIN. The order → billing sync is event-driven, so changing the
          billing cycle above does not by itself move orders already sitting on a DRAFT tagihan.
          This re-runs the sync for recent orders so they land in the right period. Issued/paid
          tagihan are never touched. */}
      {isSuperAdmin && (
        <section className="rounded-lg border bg-white p-4 text-sm shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="font-semibold text-gray-800">Sinkron Ulang Tagihan</h2>
              <p className="mt-0.5 text-xs text-gray-500">
                Hitung ulang tagihan draf klien ini dari order sejak tanggal di bawah. Jalankan
                setelah mengubah Siklus Penagihan. Tagihan yang sudah diterbitkan atau lunas tidak
                berubah.
              </p>
              <p className="mt-1 text-xs text-gray-400">
                Default: awal periode sebelumnya ({isoDateLabel(defaultResyncFrom)}) — selalu pas di
                batas periode, agar tidak ada order yang tertinggal di tengah periode.
                {resyncFromOverride && resyncFromOverride !== defaultResyncFrom && (
                  <button
                    onClick={() => setResyncFromOverride(null)}
                    className="ml-1 font-medium text-brand-700 hover:underline"
                  >
                    Kembalikan ke default
                  </button>
                )}
              </p>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="date"
                value={resyncFrom}
                onChange={(e) => setResyncFromOverride(e.target.value || null)}
                className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
              />
              <button
                onClick={() => resync.mutate()}
                disabled={resync.isPending}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
              >
                {resync.isPending ? 'Menyinkronkan…' : 'Sinkron Ulang'}
              </button>
            </div>
          </div>
        </section>
      )}

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
            + Tambah Item
          </button>
        </div>
        {(priceQ.data?.length ?? 0) > 0 && (
          <input
            type="search"
            value={priceSearch}
            onChange={(e) => setPriceSearch(e.target.value)}
            placeholder="Cari item..."
            className="mb-3 w-full max-w-sm rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        )}
        <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['Item', 'Satuan', ...(perDepartment ? ['Departemen'] : []), 'Harga', 'Berlaku Mulai', ''].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredPrices.map((price) => {
                const item = itemsById.get(price.itemId)
                return (
                  <tr key={price.itemId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-gray-800">{item?.name ?? `#${price.itemId}`}</td>
                    <td className="px-4 py-3 text-gray-500">{item ? (unitsById.get(item.unitId)?.displayName ?? '—') : '—'}</td>
                    {perDepartment && (
                      <td className="px-4 py-3 text-gray-500">
                        {price.departmentId == null
                          ? <span className="text-amber-600">Belum diatur</span>
                          : (deptById.get(price.departmentId)?.name ?? `#${price.departmentId}`)}
                      </td>
                    )}
                    <td className="px-4 py-3 font-medium text-gray-700">{rupiah(price.pricePerUnit)}</td>
                    <td className="px-4 py-3 text-gray-500">{price.effectiveDate}</td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-3">
                        <button
                          onClick={() => setPriceForm({ open: true, presetItemId: price.itemId, presetDepartmentId: price.departmentId ?? undefined, presetPrice: price.pricePerUnit, presetEffectiveDate: price.effectiveDate })}
                          className="text-sm font-medium text-brand-600 hover:text-brand-700"
                        >
                          Atur Item
                        </button>
                        <button
                          onClick={() => {
                            if (window.confirm(`Hapus item "${item?.name ?? `#${price.itemId}`}" dari klien ini?`)) {
                              deleteItem.mutate(price.itemId)
                            }
                          }}
                          disabled={deleteItem.isPending}
                          className="text-sm font-medium text-red-600 hover:text-red-700 disabled:opacity-50"
                        >
                          Hapus
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
              {priceQ.data?.length === 0 && (
                <tr><td colSpan={perDepartment ? 6 : 5} className="px-4 py-6 text-center text-sm text-gray-400">Belum ada harga.</td></tr>
              )}
              {(priceQ.data?.length ?? 0) > 0 && filteredPrices.length === 0 && (
                <tr><td colSpan={perDepartment ? 6 : 5} className="px-4 py-6 text-center text-sm text-gray-400">Tidak ada item yang cocok dengan "{priceSearch}".</td></tr>
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
        perDepartment={perDepartment}
        departments={activeDepartments}
        presetItemId={priceForm.presetItemId}
        presetDepartmentId={priceForm.presetDepartmentId}
        presetPrice={priceForm.presetPrice}
        presetEffectiveDate={priceForm.presetEffectiveDate}
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
