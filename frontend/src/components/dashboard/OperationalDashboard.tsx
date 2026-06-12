import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { monthLabelFromYm } from '../../lib/labels'
import type { FinanceTrendPoint, MonthlyBilling, MonthlyReport, OrderStatus, Page } from '../../types/api'

const rupiah = (n: number) =>
  new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)

// Compact money for tight spaces (chart labels): 1.2 jt / 950 rb.
const rupiahShort = (n: number) => {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(n >= 10_000_000 ? 0 : 1)} jt`
  if (n >= 1_000) return `${Math.round(n / 1_000)} rb`
  return String(n)
}

const now = new Date()
const YEAR = now.getFullYear()
const MONTH = now.getMonth() + 1
const monthLabel = new Intl.DateTimeFormat('id-ID', { month: 'long', year: 'numeric' }).format(now)

/**
 * FINANCE_STAFF / operational dashboard. A daily action queue (the WIP pipeline, with "Siap Antar"
 * highlighted as the orders ready to deliver) over finance figures for the month: revenue, order
 * count, average order value, outstanding receivables, a 6-month trend, and this month's top
 * clients. Money is billable orders by order date. Also shown on SUPER_ADMIN's "Operasional" toggle.
 */
export default function OperationalDashboard() {
  const { getAccessTokenSilently } = useAuth()
  const token = () => getAccessTokenSilently()

  // Pipeline counts — current open orders per status (count-only via totalElements; size=1).
  const received = useOpenCount('RECEIVED')
  const processing = useOpenCount('PROCESSING')
  const done = useOpenCount('DONE')

  const monthlyQ = useQuery({
    queryKey: ['reports', 'monthly', YEAR, MONTH],
    queryFn: async () =>
      apiFetch<MonthlyReport>(`/api/reports/monthly?year=${YEAR}&month=${MONTH}`, { token: await token() }),
  })
  const billingsQ = useQuery({
    queryKey: ['billing', 'all'],
    queryFn: async () => apiFetch<MonthlyBilling[]>('/api/billing', { token: await token() }),
  })
  const trendQ = useQuery({
    queryKey: ['dashboard', 'finance-trend'],
    queryFn: async () =>
      apiFetch<FinanceTrendPoint[]>('/api/dashboard/finance-trend?months=6', { token: await token() }),
  })

  const monthly = monthlyQ.data
  const revenue = monthly?.grandTotal ?? 0
  const orderCount = (monthly?.clients ?? []).reduce((s, c) => s + c.orderCount, 0)
  const avgOrder = orderCount > 0 ? Math.round(revenue / orderCount) : 0
  const topClients = [...(monthly?.clients ?? [])].sort((a, b) => b.total - a.total).slice(0, 5)

  const outstanding = (billingsQ.data ?? []).filter((b) => b.status === 'ISSUED')
  const outstandingTotal = outstanding.reduce((s, b) => s + b.total, 0)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-gray-800">Dasbor</h1>
        <p className="text-sm text-gray-500">Antrian harian &amp; ringkasan keuangan · {monthLabel}</p>
      </div>

      {/* Pipeline hero — the daily action queue, DONE = ready to deliver */}
      <section className="overflow-hidden rounded-2xl bg-gradient-to-br from-brand-800 via-brand-700 to-brand-500 p-5 text-white shadow-sm sm:p-6">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-blue-100/90">Antrian Order</h2>
          <Link to="/orders" className="text-xs font-medium text-blue-100/90 underline-offset-2 hover:underline">
            Lihat semua order →
          </Link>
        </div>
        <div className="flex items-stretch gap-2 sm:gap-4">
          <Stage label="Diterima" value={received} loading={received === undefined} />
          <Arrow />
          <Stage label="Diproses" value={processing} loading={processing === undefined} />
          <Arrow />
          <Stage label="Siap Antar" value={done} loading={done === undefined} highlight />
        </div>
        <p className="mt-4 text-xs text-blue-100/80">
          <span className="font-semibold text-white">Siap Antar</span> = order selesai dicuci dan menunggu diantar.
        </p>
      </section>

      {/* Finance metrics */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Metric label={`Pendapatan ${monthLabel}`} value={rupiah(revenue)} loading={monthlyQ.isLoading} to="/reports" accent="text-emerald-600" />
        <Metric label="Total Order Bulan Ini" value={orderCount.toLocaleString('id-ID')} loading={monthlyQ.isLoading} to="/orders" accent="text-brand-700" />
        <Metric label="Rata-rata / Order" value={rupiah(avgOrder)} loading={monthlyQ.isLoading} to="/reports" accent="text-blue-600" />
        <Metric
          label="Belum Lunas"
          value={rupiah(outstandingTotal)}
          sub={`${outstanding.length} tagihan terbit`}
          loading={billingsQ.isLoading}
          to="/billing"
          accent="text-amber-600"
        />
      </div>

      {/* 6-month trend + top clients */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <section className="rounded-xl border border-gray-100 bg-white p-5 shadow-sm lg:col-span-2">
          <h2 className="mb-4 text-sm font-semibold text-gray-700">Tren 6 Bulan (pendapatan &amp; jumlah order)</h2>
          <TrendChart points={trendQ.data} loading={trendQ.isLoading} />
        </section>

        <section className="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-gray-700">Klien Teratas Bulan Ini</h2>
            <Link to="/reports" className="text-xs font-medium text-brand-700 hover:underline">Laporan →</Link>
          </div>
          {monthlyQ.isLoading ? (
            <p className="text-sm text-gray-400">Memuat…</p>
          ) : topClients.length === 0 ? (
            <p className="text-sm text-gray-400">Belum ada order bulan ini.</p>
          ) : (
            <ol className="space-y-2">
              {topClients.map((c, i) => (
                <li key={c.clientId} className="flex items-center gap-3 text-sm">
                  <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-gray-100 text-xs font-semibold text-gray-500">
                    {i + 1}
                  </span>
                  <span className="min-w-0 flex-1 truncate text-gray-700">{c.clientName}</span>
                  <span className="shrink-0 text-right">
                    <span className="block font-medium text-gray-800">{rupiah(c.total)}</span>
                    <span className="block text-xs text-gray-400">{c.orderCount} order</span>
                  </span>
                </li>
              ))}
            </ol>
          )}
        </section>
      </div>
    </div>
  )
}

/** Current count of orders in a given status (count-only via Page.totalElements). */
function useOpenCount(status: OrderStatus): number | undefined {
  const { getAccessTokenSilently } = useAuth()
  return useQuery({
    queryKey: ['orders', 'count', status],
    queryFn: async () =>
      apiFetch<Page<unknown>>(`/api/orders?status=${status}&size=1`, { token: await getAccessTokenSilently() }),
  }).data?.totalElements
}

function Stage({ label, value, loading, highlight }: { label: string; value?: number; loading: boolean; highlight?: boolean }) {
  return (
    <div
      className={`flex flex-1 flex-col items-center justify-center rounded-xl px-2 py-4 text-center ${
        highlight ? 'bg-white/20 ring-1 ring-white/40' : 'bg-white/10'
      }`}
    >
      <span className={`font-bold leading-none ${highlight ? 'text-4xl sm:text-5xl' : 'text-3xl sm:text-4xl'}`}>
        {loading ? '·' : (value ?? 0).toLocaleString('id-ID')}
      </span>
      <span className="mt-2 text-xs font-medium text-blue-100/90">{label}</span>
    </div>
  )
}

function Arrow() {
  return <div className="flex items-center text-2xl text-white/40">→</div>
}

type MetricProps = { label: string; value: string; sub?: string; loading: boolean; to: string; accent: string }
function Metric({ label, value, sub, loading, to, accent }: MetricProps) {
  return (
    <Link to={to} className="rounded-xl border border-gray-100 bg-white p-5 shadow-sm transition-shadow hover:shadow-md">
      <p className="text-xs font-medium uppercase tracking-wide text-gray-400">{label}</p>
      <p className={`mt-2 text-2xl font-bold ${accent}`}>
        {loading ? <span className="inline-block h-7 w-24 animate-pulse rounded bg-gray-100" /> : value}
      </p>
      {sub && <p className="mt-1 text-xs text-gray-400">{sub}</p>}
    </Link>
  )
}

function TrendChart({ points, loading }: { points?: FinanceTrendPoint[]; loading: boolean }) {
  if (loading) return <p className="text-sm text-gray-400">Memuat…</p>
  if (!points || points.length === 0) return <p className="text-sm text-gray-400">Belum ada data.</p>
  const max = Math.max(...points.map((p) => p.revenue), 1)
  return (
    <div className="flex items-end justify-between gap-2" style={{ height: 180 }}>
      {points.map((p) => (
        <div key={p.month} className="flex flex-1 flex-col items-center justify-end gap-1">
          <span className="text-[10px] font-medium text-gray-500">{rupiahShort(p.revenue)}</span>
          <div
            className="w-full max-w-[40px] rounded-t-md bg-gradient-to-t from-brand-600 to-brand-400"
            style={{ height: `${Math.max((p.revenue / max) * 130, 2)}px` }}
            title={`${monthLabelFromYm(p.month)} · ${rupiah(p.revenue)} · ${p.orderCount} order`}
          />
          <span className="text-[10px] text-gray-400">{monthLabelFromYm(p.month, true)}</span>
          <span className="text-[10px] font-medium text-gray-600">{p.orderCount} ord</span>
        </div>
      ))}
    </div>
  )
}
