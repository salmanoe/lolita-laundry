import { lazy, Suspense, useState } from 'react'
import { useMe } from '../auth/useMe'
import OperationalDashboard from '../components/dashboard/OperationalDashboard'

// Recharts is pulled in only via this lazy import, so STAFF / non-dashboard navigation never pays for it.
const AnalyticalDashboard = lazy(() => import('../components/dashboard/AnalyticalDashboard'))

const spinner = (
  <div className="flex h-64 items-center justify-center">
    <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
  </div>
)

function analytical() {
  return <Suspense fallback={spinner}>{<AnalyticalDashboard />}</Suspense>
}

/**
 * Role-aware dashboard.
 * - STAFF → operational summary only.
 * - OWNER → analytical dashboard only.
 * - SUPER_ADMIN → both, switchable via a toggle (defaults to analytical).
 * - Unresolved dev/mock role fails open to the analytical view.
 * DRIVER never reaches here — Layout redirects them to /deliveries.
 */
export default function DashboardPage() {
  const meQ = useMe()
  const [view, setView] = useState<'analytical' | 'operational'>('analytical')

  if (meQ.isLoading) {
    return spinner
  }

  const role = meQ.data?.role

  if (role === 'STAFF') {
    return <OperationalDashboard />
  }

  if (role === 'OWNER') {
    return analytical()
  }

  // SUPER_ADMIN (and unresolved dev/mock role) — full access to both views.
  if (role === 'SUPER_ADMIN') {
    return (
      <div>
        <div className="mb-4 inline-flex rounded-lg border border-gray-200 bg-white p-1 text-sm">
          <button
            onClick={() => setView('analytical')}
            className={`rounded-md px-3 py-1.5 font-medium transition-colors ${
              view === 'analytical' ? 'bg-brand-600 text-white' : 'text-gray-600 hover:bg-gray-50'
            }`}
          >
            Analitik
          </button>
          <button
            onClick={() => setView('operational')}
            className={`rounded-md px-3 py-1.5 font-medium transition-colors ${
              view === 'operational' ? 'bg-brand-600 text-white' : 'text-gray-600 hover:bg-gray-50'
            }`}
          >
            Operasional
          </button>
        </div>
        {view === 'operational' ? <OperationalDashboard /> : analytical()}
      </div>
    )
  }

  return analytical()
}