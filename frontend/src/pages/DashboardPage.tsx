import { lazy, Suspense } from 'react'
import { useMe } from '../auth/useMe'
import OperationalDashboard from '../components/dashboard/OperationalDashboard'

// Recharts is pulled in only via this lazy import, so STAFF / non-dashboard navigation never pays for it.
const AnalyticalDashboard = lazy(() => import('../components/dashboard/AnalyticalDashboard'))

/**
 * Role-aware dashboard. STAFF get the operational summary; OWNER (and an unresolved dev/mock role,
 * which fails open to the richer view) get the analytical dashboard. DRIVER never reaches here —
 * Layout redirects them to /deliveries.
 */
export default function DashboardPage() {
  const meQ = useMe()

  if (meQ.isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
      </div>
    )
  }

  if (meQ.data?.role === 'STAFF') {
    return <OperationalDashboard />
  }

  return (
    <Suspense
      fallback={
        <div className="flex h-64 items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
        </div>
      }
    >
      <AnalyticalDashboard />
    </Suspense>
  )
}