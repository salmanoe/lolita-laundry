import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { monthLabelFromYm } from '../../lib/labels'
import { rupiahCompact } from '../../lib/money'
import { colorFor } from '../../lib/clientColors'
import RevenueTrendChart from './RevenueTrendChart'
import MonthlyBreakdownChart from './MonthlyBreakdownChart'
import type { DashboardAnalytics } from '../../types/api'

type RangeKey = 'ytd' | '6m' | '12m'

const RANGE_OPTIONS: { key: RangeKey; label: string }[] = [
  { key: 'ytd', label: 'Tahun Ini' },
  { key: '6m', label: '6 Bulan' },
  { key: '12m', label: '12 Bulan' },
]

const isoDate = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

function computeRange(key: RangeKey): { from: string; to: string } {
  const now = new Date()
  const to = isoDate(now)
  if (key === 'ytd') return { from: `${now.getFullYear()}-01-01`, to }
  const back = key === '6m' ? 5 : 11
  return { from: isoDate(new Date(now.getFullYear(), now.getMonth() - back, 1)), to }
}

/**
 * Business-analytics dashboard for the OWNER (lazy-loaded — pulls in Recharts only here). Money is
 * billable orders by order date (same basis as Reports). Backed by GET /api/dashboard/analytics.
 */
export default function AnalyticalDashboard() {
  const { getAccessTokenSilently } = useAuth()
  const [range, setRange] = useState<RangeKey>('ytd')
  const { from, to } = computeRange(range)

  const { data, isLoading, error } = useQuery({
    queryKey: ['dashboard', 'analytics', from, to],
    queryFn: async () =>
      apiFetch<DashboardAnalytics>(`/api/dashboard/analytics?from=${from}&to=${to}`, {
        token: await getAccessTokenSilently(),
      }),
  })

  const rangeLabel = `${monthLabelFromYm(from.slice(0, 7))} – ${monthLabelFromYm(to.slice(0, 7))}`
  const empty = !!data && data.hotels.length === 0

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-gray-800">Dasbor</h1>
          <p className="text-sm text-gray-500">Ringkasan bisnis — {rangeLabel}</p>
        </div>
        <div className="inline-flex rounded-lg border border-gray-200 bg-white p-0.5 text-sm shadow-sm">
          {RANGE_OPTIONS.map((o) => (
            <button
              key={o.key}
              onClick={() => setRange(o.key)}
              className={`rounded-md px-3 py-1.5 font-medium transition-colors ${
                range === o.key ? 'bg-brand-600 text-white' : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              {o.label}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {error instanceof ApiError ? error.detail : 'Gagal memuat analitik.'}
        </div>
      )}

      {/* KPI cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <KpiCard label="Total Pendapatan" loading={isLoading}
          value={data && rupiahCompact(data.totalRevenue)} subtitle={rangeLabel} />
        <KpiCard label="Total Order" loading={isLoading}
          value={data && data.totalOrders.toLocaleString('id-ID')}
          subtitle={data ? `di ${data.hotels.length} hotel` : undefined} />
        <KpiCard label="Rata-rata per Order" loading={isLoading}
          value={data && rupiahCompact(data.avgOrderValue)} subtitle="per order" />
        <KpiCard label="Bulan Terbaik" loading={isLoading}
          value={data && (data.bestMonth ? monthLabelFromYm(data.bestMonth.month) : '—')}
          subtitle={data?.bestMonth ? rupiahCompact(data.bestMonth.revenue) : undefined} />
      </div>

      {empty ? (
        <div className="rounded-xl border border-gray-100 bg-white p-10 text-center text-sm text-gray-400">
          Belum ada order pada rentang ini.
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            {/* Revenue by hotel */}
            <section className="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
              <h2 className="mb-4 text-xs font-semibold uppercase tracking-wide text-gray-500">Pendapatan per Hotel</h2>
              {isLoading ? (
                <ListSkeleton />
              ) : (
                <ul className="divide-y divide-gray-100">
                  {data!.hotels.map((h, i) => (
                    <li key={h.clientId} className="flex items-center gap-3 py-2.5">
                      <span className="h-2.5 w-2.5 shrink-0 rounded-full" style={{ backgroundColor: colorFor(i) }} />
                      <span className="min-w-0 flex-1 truncate text-sm text-gray-700">{h.name}</span>
                      <span className="text-sm font-semibold text-gray-800">{rupiahCompact(h.revenue)}</span>
                      <span className="w-16 text-right text-xs text-gray-400">{h.orderCount} order</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            {/* Monthly revenue trend */}
            <section className="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
              <h2 className="mb-4 text-xs font-semibold uppercase tracking-wide text-gray-500">Tren Pendapatan Bulanan</h2>
              {isLoading ? <ChartSkeleton h={260} /> : <RevenueTrendChart months={data!.months} />}
            </section>
          </div>

          {/* Monthly per-hotel breakdown */}
          <section className="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
            <h2 className="mb-4 text-xs font-semibold uppercase tracking-wide text-gray-500">Rincian Bulanan per Hotel</h2>
            {isLoading ? <ChartSkeleton h={340} /> : <MonthlyBreakdownChart hotels={data!.hotels} months={data!.months} />}
            <p className="mt-3 text-xs text-gray-400">* bulan berjalan (belum penuh)</p>
          </section>
        </>
      )}
    </div>
  )
}

function KpiCard({
  label,
  value,
  subtitle,
  loading,
}: {
  label: string
  value: string | undefined | null | false
  subtitle?: string
  loading: boolean
}) {
  return (
    <div className="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wide text-gray-400">{label}</p>
      {loading ? (
        <span className="mt-3 inline-block h-7 w-24 animate-pulse rounded bg-gray-100" />
      ) : (
        <p className="mt-2 text-2xl font-bold text-gray-800">{value || '—'}</p>
      )}
      {subtitle && !loading && <p className="mt-1 text-xs text-gray-400">{subtitle}</p>}
    </div>
  )
}

function ChartSkeleton({ h }: { h: number }) {
  return <div className="w-full animate-pulse rounded-lg bg-gray-50" style={{ height: h }} />
}

function ListSkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="h-5 w-full animate-pulse rounded bg-gray-50" />
      ))}
    </div>
  )
}