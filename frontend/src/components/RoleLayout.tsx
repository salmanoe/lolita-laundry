import { Navigate, useLocation } from 'react-router-dom'
import { useMe } from '../auth/useMe'
import Layout from './Layout'
import OperatorLayout from './OperatorLayout'

/**
 * Picks the app chrome by role for the authenticated route tree:
 * - DAILY_STAFF → the minimal {@link OperatorLayout} (Buat Order / Order / Pengantaran). They manage
 *   orders fully (create / edit / advance / cancel), so order-detail (`/orders/{id}`) is allowed too;
 *   anything outside the order + delivery screens redirects back to Buat Order.
 * - everyone else (FINANCE_STAFF / SUPER_ADMIN / unresolved dev role) → the admin {@link Layout}.
 *
 * The backend @PreAuthorize is the real enforcement; this is UX (don't render dead/403 screens).
 */
const dailyStaffAllowed = (path: string) =>
  path === '/orders' || path === '/orders/new' || path === '/deliveries' || /^\/orders\/\d+$/.test(path)

export default function RoleLayout() {
  const meQ = useMe()
  const location = useLocation()

  if (meQ.isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-gray-50">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
      </div>
    )
  }

  if (meQ.data?.role === 'DAILY_STAFF') {
    if (!dailyStaffAllowed(location.pathname)) {
      return <Navigate to="/orders/new" replace />
    }
    return <OperatorLayout />
  }

  return <Layout />
}
