import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { monthName, orderStatusBadge, orderStatusLabel } from '../lib/labels'
import type { Client, DailyReport, HotelReport, MonthlyReport, Page } from '../types/api'

const rupiah = (n: number) =>
  new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)

const today = () => new Date().toISOString().slice(0, 10)
const YEARS = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i)
const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1)

type Tab = 'daily' | 'monthly' | 'hotel'

const TABS: { key: Tab; label: string }[] = [
  { key: 'daily', label: 'Harian' },
  { key: 'monthly', label: 'Bulanan' },
  { key: 'hotel', label: 'Per Klien' },
]

export default function ReportsPage() {
  const [tab, setTab] = useState<Tab>('daily')

  return (
    <div>
      <h1 className="mb-1 text-xl font-semibold text-gray-800">Laporan</h1>
      <p className="mb-5 text-sm text-gray-500">Berdasarkan order yang masuk (tanggal order)</p>

      <div className="mb-5 inline-flex rounded-lg border border-gray-200 bg-white p-1 shadow-sm">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`rounded-md px-4 py-1.5 text-sm font-medium transition-colors ${
              tab === t.key ? 'bg-brand-700 text-white shadow-sm' : 'text-gray-600 hover:bg-gray-50'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'daily' && <DailyTab />}
      {tab === 'monthly' && <MonthlyTab />}
      {tab === 'hotel' && <HotelTab />}
    </div>
  )
}

// ── Shared bits ────────────────────────────────────────────────────────────────

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-xs font-medium text-gray-500">{label}</span>
      {children}
    </label>
  )
}

const inputClass =
  'rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'

function Loading() {
  return <div className="rounded-lg border border-gray-100 bg-white p-8 text-center text-sm text-gray-400">Memuat…</div>
}

function ErrorBox() {
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">Gagal memuat laporan.</div>
  )
}

function Empty({ text }: { text: string }) {
  return (
    <div className="rounded-lg border-2 border-dashed border-gray-200 p-8 text-center text-sm text-gray-400">{text}</div>
  )
}

// ── Per-client table (daily + monthly share this) ────────────────────────────────

function ClientTotalsTable({ rows, grandTotal }: { rows: DailyReport['clients']; grandTotal: number }) {
  if (rows.length === 0) return <Empty text="Tidak ada order pada periode ini." />
  return (
    <div className="overflow-hidden rounded-xl border border-gray-100 bg-white shadow-sm">
      <table className="w-full text-sm">
        <thead className="bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-400">
          <tr>
            <th className="px-4 py-2.5 font-medium">Klien</th>
            <th className="px-4 py-2.5 text-right font-medium">Jml Order</th>
            <th className="px-4 py-2.5 text-right font-medium">Total</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {rows.map((c) => (
            <tr key={c.clientId} className="hover:bg-gray-50/60">
              <td className="px-4 py-2.5 text-gray-800">
                {c.clientName}
                {c.clientCode && <span className="ml-2 text-xs text-gray-400">{c.clientCode}</span>}
              </td>
              <td className="px-4 py-2.5 text-right tabular-nums text-gray-600">{c.orderCount}</td>
              <td className="px-4 py-2.5 text-right tabular-nums font-medium text-gray-800">{rupiah(c.total)}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr className="border-t-2 border-gray-100 bg-gray-50/50 font-semibold text-gray-800">
            <td className="px-4 py-3">Total</td>
            <td className="px-4 py-3 text-right tabular-nums">{rows.reduce((s, c) => s + c.orderCount, 0)}</td>
            <td className="px-4 py-3 text-right tabular-nums">{rupiah(grandTotal)}</td>
          </tr>
        </tfoot>
      </table>
    </div>
  )
}

// ── Daily ────────────────────────────────────────────────────────────────────────

function DailyTab() {
  const { getAccessTokenSilently } = useAuth()
  const [date, setDate] = useState(today())

  const { data, isLoading, error } = useQuery({
    queryKey: ['report', 'daily', date],
    queryFn: async () =>
      apiFetch<DailyReport>(`/api/reports/daily?date=${date}`, { token: await getAccessTokenSilently() }),
  })

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-3">
        <Field label="Tanggal">
          <input type="date" value={date} max={today()} onChange={(e) => setDate(e.target.value)} className={inputClass} />
        </Field>
      </div>
      {isLoading ? <Loading /> : error ? <ErrorBox /> : data && <ClientTotalsTable rows={data.clients} grandTotal={data.grandTotal} />}
    </div>
  )
}

// ── Monthly ────────────────────────────────────────────────────────────────────────

function MonthlyTab() {
  const { getAccessTokenSilently } = useAuth()
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1)

  const { data, isLoading, error } = useQuery({
    queryKey: ['report', 'monthly', year, month],
    queryFn: async () =>
      apiFetch<MonthlyReport>(`/api/reports/monthly?year=${year}&month=${month}`, {
        token: await getAccessTokenSilently(),
      }),
  })

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-3">
        <Field label="Bulan">
          <select value={month} onChange={(e) => setMonth(Number(e.target.value))} className={inputClass}>
            {MONTHS.map((m) => (
              <option key={m} value={m}>
                {monthName[m]}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Tahun">
          <select value={year} onChange={(e) => setYear(Number(e.target.value))} className={inputClass}>
            {YEARS.map((y) => (
              <option key={y} value={y}>
                {y}
              </option>
            ))}
          </select>
        </Field>
      </div>
      {isLoading ? <Loading /> : error ? <ErrorBox /> : data && <ClientTotalsTable rows={data.clients} grandTotal={data.grandTotal} />}
    </div>
  )
}

// ── Per-hotel ────────────────────────────────────────────────────────────────────────

function HotelTab() {
  const { getAccessTokenSilently } = useAuth()
  const firstOfMonth = new Date()
  firstOfMonth.setDate(1)
  const [clientId, setClientId] = useState<number | ''>('')
  const [from, setFrom] = useState(firstOfMonth.toISOString().slice(0, 10))
  const [to, setTo] = useState(today())

  const clientsQ = useQuery({
    queryKey: ['clients', 'options'],
    queryFn: async () =>
      apiFetch<Page<Client>>('/api/clients?page=0&size=200&sort=name', { token: await getAccessTokenSilently() }),
  })

  const enabled = clientId !== '' && !!from && !!to
  const { data, isLoading, error } = useQuery({
    enabled,
    queryKey: ['report', 'hotel', clientId, from, to],
    queryFn: async () =>
      apiFetch<HotelReport>(`/api/reports/hotel/${clientId}?from=${from}&to=${to}`, {
        token: await getAccessTokenSilently(),
      }),
  })

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-3">
        <Field label="Klien">
          <select value={clientId} onChange={(e) => setClientId(e.target.value === '' ? '' : Number(e.target.value))} className={inputClass}>
            <option value="">— Pilih klien —</option>
            {(clientsQ.data?.content ?? []).map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Dari">
          <input type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} className={inputClass} />
        </Field>
        <Field label="Sampai">
          <input type="date" value={to} max={today()} onChange={(e) => setTo(e.target.value)} className={inputClass} />
        </Field>
      </div>

      {!enabled ? (
        <Empty text="Pilih klien dan rentang tanggal untuk melihat laporan." />
      ) : isLoading ? (
        <Loading />
      ) : error ? (
        <ErrorBox />
      ) : (
        data && <HotelReportView report={data} />
      )}
    </div>
  )
}

function HotelReportView({ report }: { report: HotelReport }) {
  if (report.orders.length === 0) return <Empty text="Tidak ada order pada rentang ini." />
  return (
    <div className="space-y-5">
      <div className="rounded-xl border border-gray-100 bg-white p-4 shadow-sm">
        <p className="text-sm text-gray-500">
          <span className="font-medium text-gray-800">{report.clientName}</span>
          {report.clientCode && <span className="ml-2 text-xs text-gray-400">{report.clientCode}</span>}
        </p>
        <p className="mt-1 text-2xl font-bold text-gray-800">{rupiah(report.grandTotal)}</p>
        <p className="text-xs text-gray-400">{report.orders.length} order</p>
      </div>

      {/* Item breakdown */}
      <div>
        <h2 className="mb-2 text-sm font-semibold text-gray-700">Rincian Item</h2>
        <div className="overflow-hidden rounded-xl border border-gray-100 bg-white shadow-sm">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-400">
              <tr>
                <th className="px-4 py-2.5 font-medium">Item</th>
                <th className="px-4 py-2.5 text-right font-medium">Qty</th>
                <th className="px-4 py-2.5 text-right font-medium">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {report.items.map((i) => (
                <tr key={i.itemName} className="hover:bg-gray-50/60">
                  <td className="px-4 py-2.5 text-gray-800">{i.itemName}</td>
                  <td className="px-4 py-2.5 text-right tabular-nums text-gray-600">
                    {i.quantity}
                    {i.unit && <span className="ml-1 text-xs text-gray-400">{i.unit}</span>}
                  </td>
                  <td className="px-4 py-2.5 text-right tabular-nums font-medium text-gray-800">{rupiah(i.total)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Orders */}
      <div>
        <h2 className="mb-2 text-sm font-semibold text-gray-700">Daftar Order</h2>
        <div className="overflow-hidden rounded-xl border border-gray-100 bg-white shadow-sm">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-400">
              <tr>
                <th className="px-4 py-2.5 font-medium">No. Order</th>
                <th className="px-4 py-2.5 font-medium">Tanggal</th>
                <th className="px-4 py-2.5 font-medium">Status</th>
                <th className="px-4 py-2.5 text-right font-medium">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {report.orders.map((o) => (
                <tr key={o.orderId} className="hover:bg-gray-50/60">
                  <td className="px-4 py-2.5 font-medium text-gray-800">{o.orderNumber}</td>
                  <td className="px-4 py-2.5 text-gray-600">{o.orderDate}</td>
                  <td className="px-4 py-2.5">
                    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${orderStatusBadge[o.status]}`}>
                      {orderStatusLabel[o.status]}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 text-right tabular-nums font-medium text-gray-800">{rupiah(o.total)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}