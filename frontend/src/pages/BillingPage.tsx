import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useMe } from '../auth/useMe'
import Modal from '../components/Modal'
import { billingStatusBadge, billingStatusLabel, monthName } from '../lib/labels'
import type { Client, MonthlyBilling, Page } from '../types/api'

const rupiah = (n: number) =>
  new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)

const YEARS = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i)
const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1)

export default function BillingPage() {
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const isOwner = useMe().data?.role === 'OWNER'
  const [clientId, setClientId] = useState<number | ''>('')
  const [year, setYear] = useState<number | ''>('')
  const [month, setMonth] = useState<number | ''>('')
  const [genOpen, setGenOpen] = useState(false)

  // OWNER-only bulk refresh: re-render every billing & invoice PDF to the current template
  // (layout-only — amounts unchanged). For applying a finalized PDF design before go-live.
  const regenAll = useMutation({
    mutationFn: async () =>
      apiFetch<{ billings: number; invoices: number }>('/api/billing/regenerate-all-pdfs', {
        method: 'POST',
        token: await getAccessTokenSilently(),
      }),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ['billings'] })
      qc.invalidateQueries({ queryKey: ['billing'] })
      alert(`PDF diperbarui: ${r.billings} tagihan, ${r.invoices} invoice.`)
    },
    onError: (e) => alert(e instanceof ApiError ? e.detail : 'Gagal memperbarui PDF.'),
  })

  // Client list for the filter dropdown and for resolving names in rows.
  const clientsQ = useQuery({
    queryKey: ['clients', 'options'],
    queryFn: async () =>
      apiFetch<Page<Client>>('/api/clients?page=0&size=200&sort=name', { token: await getAccessTokenSilently() }),
  })
  const clientsById = new Map((clientsQ.data?.content ?? []).map((c) => [c.id, c]))

  const params = new URLSearchParams()
  if (clientId !== '') params.set('clientId', String(clientId))
  if (year !== '') params.set('year', String(year))
  if (month !== '') params.set('month', String(month))

  const { data, isLoading, error } = useQuery({
    queryKey: ['billings', { clientId, year, month }],
    queryFn: async () =>
      apiFetch<MonthlyBilling[]>(`/api/billing?${params}`, { token: await getAccessTokenSilently() }),
  })

  const billings = data ?? []

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold text-gray-800">Tagihan Bulanan</h1>
        <div className="flex gap-2">
          {isOwner && (
            <button
              onClick={() => {
                if (window.confirm('Perbarui semua PDF tagihan & invoice ke format terbaru? Nominal tidak berubah.')) {
                  regenAll.mutate()
                }
              }}
              disabled={regenAll.isPending}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
            >
              {regenAll.isPending ? 'Memperbarui…' : 'Perbarui Semua PDF'}
            </button>
          )}
          <button
            onClick={() => setGenOpen(true)}
            className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
          >
            Buat Tagihan
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="mb-4 grid grid-cols-2 gap-3 md:grid-cols-3">
        <select
          value={clientId}
          onChange={(e) => setClientId(e.target.value === '' ? '' : Number(e.target.value))}
          className={filterCls}
        >
          <option value="">Semua Klien</option>
          {(clientsQ.data?.content ?? []).map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
        <select
          value={year}
          onChange={(e) => setYear(e.target.value === '' ? '' : Number(e.target.value))}
          className={filterCls}
        >
          <option value="">Semua Tahun</option>
          {YEARS.map((y) => <option key={y} value={y}>{y}</option>)}
        </select>
        <select
          value={month}
          onChange={(e) => setMonth(e.target.value === '' ? '' : Number(e.target.value))}
          className={filterCls}
        >
          <option value="">Semua Bulan</option>
          {MONTHS.map((m) => <option key={m} value={m}>{monthName[m]}</option>)}
        </select>
      </div>

      {isLoading ? (
        <div className="text-sm text-gray-400">Memuat data tagihan...</div>
      ) : error ? (
        <div className="text-sm text-red-500">Gagal memuat data tagihan.</div>
      ) : (
        <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['No. Tagihan', 'Klien', 'Periode', 'Total', 'Status'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {billings.map((b) => (
                <tr key={b.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <Link to={`/billing/${b.id}`} className="font-mono font-medium text-brand-700 hover:underline">
                      {b.billingNumber}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-gray-700">{clientsById.get(b.clientId)?.name ?? `#${b.clientId}`}</td>
                  <td className="px-4 py-3 text-gray-500">{monthName[b.periodMonth]} {b.periodYear}</td>
                  <td className="px-4 py-3 font-medium text-gray-700">{rupiah(b.total)}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${billingStatusBadge[b.status]}`}>
                      {billingStatusLabel[b.status]}
                    </span>
                  </td>
                </tr>
              ))}
              {billings.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-sm text-gray-400">
                    Belum ada tagihan yang cocok dengan filter.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      <GenerateBillingModal
        open={genOpen}
        clients={clientsQ.data?.content ?? []}
        onClose={() => setGenOpen(false)}
      />
    </div>
  )
}

function GenerateBillingModal({
  open, clients, onClose,
}: {
  open: boolean
  clients: Client[]
  onClose: () => void
}) {
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const now = new Date()
  const [clientId, setClientId] = useState<number | ''>('')
  // Default to the previous month — billing is usually run after a period closes.
  const [year, setYear] = useState(now.getMonth() === 0 ? now.getFullYear() - 1 : now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() === 0 ? 12 : now.getMonth())

  const generate = useMutation({
    mutationFn: async () =>
      apiFetch<MonthlyBilling[]>('/api/billing/generate', {
        method: 'POST',
        token: await getAccessTokenSilently(),
        body: JSON.stringify({ clientId, year, month }),
      }),
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: ['billings'] })
      onClose()
      if (created.length === 0) {
        alert('Tidak ada order DELIVERED pada periode tersebut — tidak ada tagihan yang dibuat.')
      }
    },
  })

  return (
    <Modal open={open} title="Buat Tagihan Bulanan" onClose={onClose}>
      <div className="space-y-4">
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">Klien</label>
          <select
            value={clientId}
            onChange={(e) => setClientId(e.target.value === '' ? '' : Number(e.target.value))}
            className={fieldCls}
          >
            <option value="">Pilih klien…</option>
            {clients.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Tahun</label>
            <select value={year} onChange={(e) => setYear(Number(e.target.value))} className={fieldCls}>
              {YEARS.map((y) => <option key={y} value={y}>{y}</option>)}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Bulan</label>
            <select value={month} onChange={(e) => setMonth(Number(e.target.value))} className={fieldCls}>
              {MONTHS.map((m) => <option key={m} value={m}>{monthName[m]}</option>)}
            </select>
          </div>
        </div>

        <p className="text-xs text-gray-400">
          Menghasilkan ulang periode yang sudah ada akan menimpa tagihan berstatus Draf, tetapi ditolak jika tagihan
          sudah Terbit atau Lunas. Klien PBS menghasilkan satu tagihan per departemen.
        </p>

        {generate.error && (
          <p className="text-sm text-red-500">
            {generate.error instanceof ApiError ? generate.error.detail : 'Gagal membuat tagihan.'}
          </p>
        )}

        <div className="flex justify-end gap-2 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Batal
          </button>
          <button
            type="button"
            disabled={clientId === '' || generate.isPending}
            onClick={() => generate.mutate()}
            className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-50"
          >
            {generate.isPending ? 'Memproses…' : 'Buat'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

const filterCls =
  'rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'
const fieldCls =
  'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'
