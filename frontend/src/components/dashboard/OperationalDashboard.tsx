import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { BasketIcon, ChartIcon, InvoiceIcon, TowelsIcon } from '../NavIcons'
import type { DashboardSummary } from '../../types/api'

const rupiah = (n: number) =>
  new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)

const monthLabel = new Intl.DateTimeFormat('id-ID', { month: 'long', year: 'numeric' }).format(new Date())

/**
 * Operational dashboard — at-a-glance state for STAFF (orders today, in-progress, ready, revenue
 * this month). Money is billable orders by order date. Backed by GET /api/dashboard/summary.
 */
export default function OperationalDashboard() {
  const { getAccessTokenSilently } = useAuth()

  const { data, isLoading, error } = useQuery({
    queryKey: ['dashboard', 'summary'],
    queryFn: async () =>
      apiFetch<DashboardSummary>('/api/dashboard/summary', { token: await getAccessTokenSilently() }),
  })

  return (
    <div>
      <h1 className="mb-1 text-xl font-semibold text-gray-800">Dasbor</h1>
      <p className="mb-6 text-sm text-gray-500">Ringkasan operasional hari ini</p>

      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          Gagal memuat ringkasan.
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Card
          label="Order Hari Ini"
          value={data?.ordersToday}
          loading={isLoading}
          Icon={BasketIcon}
          to="/orders"
          accent="from-brand-700 to-brand-500"
        />
        <Card
          label="Sedang Diproses"
          value={data?.inProgress}
          loading={isLoading}
          Icon={TowelsIcon}
          to="/orders"
          accent="from-blue-600 to-blue-400"
        />
        <Card
          label="Siap Antar"
          value={data?.readyForDelivery}
          loading={isLoading}
          Icon={ChartIcon}
          to="/orders"
          accent="from-violet-600 to-violet-400"
        />
        <Card
          label={`Pendapatan ${monthLabel}`}
          value={data?.revenueThisMonth}
          loading={isLoading}
          Icon={InvoiceIcon}
          to="/reports"
          money
          accent="from-emerald-600 to-emerald-400"
        />
      </div>

      <div className="mt-6 rounded-xl border border-gray-100 bg-white p-5 text-sm text-gray-500">
        Butuh rincian per klien atau per hari?{' '}
        <Link to="/reports" className="font-medium text-brand-700 hover:underline">
          Buka Laporan →
        </Link>
      </div>
    </div>
  )
}

type CardProps = {
  label: string
  value: number | undefined
  loading: boolean
  Icon: React.ComponentType<{ className?: string }>
  to: string
  money?: boolean
  accent: string
}

function Card({ label, value, loading, Icon, to, money, accent }: CardProps) {
  return (
    <Link
      to={to}
      className="group relative overflow-hidden rounded-xl border border-gray-100 bg-white p-5 shadow-sm transition-shadow hover:shadow-md"
    >
      <div
        className={`absolute right-0 top-0 flex h-12 w-12 -translate-y-2 translate-x-2 items-center justify-center rounded-bl-2xl bg-gradient-to-br ${accent} text-white opacity-90`}
      >
        <Icon className="h-5 w-5" />
      </div>
      <p className="pr-12 text-xs font-medium uppercase tracking-wide text-gray-400">{label}</p>
      <p className="mt-3 text-2xl font-bold text-gray-800">
        {loading ? (
          <span className="inline-block h-7 w-20 animate-pulse rounded bg-gray-100" />
        ) : value === undefined ? (
          '—'
        ) : money ? (
          rupiah(value)
        ) : (
          value.toLocaleString('id-ID')
        )}
      </p>
    </Link>
  )
}