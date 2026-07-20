import { useEffect, useRef } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { apiFetch, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useMe } from '../auth/useMe'
import Layout from './Layout'
import OperatorLayout from './OperatorLayout'
import PendingApprovalScreen from '../pages/PendingApprovalScreen'
import DeactivatedScreen from '../pages/DeactivatedScreen'

/**
 * Picks the app chrome by role for the authenticated route tree:
 * - DAILY_STAFF → the minimal {@link OperatorLayout} (Buat Order / Order / Pengantaran). They manage
 *   orders fully (create / edit / advance / cancel), so order-detail (`/orders/{id}`) is allowed too;
 *   anything outside the order + delivery screens redirects back to Buat Order.
 * - everyone else (FINANCE_STAFF / SUPER_ADMIN) → the admin {@link Layout}.
 *
 * Unprovisioned identity (GET /api/me → 204): with real Auth0, self-register the caller into the
 * pending queue (once) and show {@link PendingApprovalScreen} until a SUPER_ADMIN assigns a role. In
 * mock/dev mode (VITE_AUTH_MOCK=true) the null role still falls open to the admin Layout, unchanged.
 *
 * The backend @PreAuthorize is the real enforcement; this is UX (don't render dead/403 screens).
 */
const dailyStaffAllowed = (path: string) =>
  path === '/orders' || path === '/orders/new' || path === '/deliveries' || /^\/orders\/\d+$/.test(path)

const isMock = import.meta.env.VITE_AUTH_MOCK === 'true'

export default function RoleLayout() {
  const meQ = useMe()
  const location = useLocation()
  const { user, getAccessTokenSilently } = useAuth()
  const registered = useRef(false)

  // A deactivated account gets 403 "account_deactivated" from GET /api/me (see UserController): the
  // backend revoked them even though their Auth0 session is still valid. Distinct from unprovisioned
  // (204 → null data) so they don't get funneled into the self-register/pending flow below.
  const isDeactivated =
    meQ.error instanceof ApiError && meQ.error.status === 403 && meQ.error.detail === 'account_deactivated'

  const unprovisioned = !meQ.isLoading && !meQ.isError && meQ.data == null

  // First login with real Auth0: queue this identity for SUPER_ADMIN approval (idempotent, fire once).
  useEffect(() => {
    if (isMock || !unprovisioned || registered.current) return
    registered.current = true
    void (async () => {
      try {
        const token = await getAccessTokenSilently()
        await apiFetch('/api/me/register', {
          method: 'POST',
          token,
          body: JSON.stringify({ email: user?.email ?? null, fullName: user?.name ?? null }),
        })
      } catch {
        // Best-effort: if it fails the admin won't see them this time, but the next login retries.
      }
    })()
  }, [unprovisioned, user, getAccessTokenSilently])

  if (meQ.isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-gray-50">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
      </div>
    )
  }

  if (isDeactivated) {
    return <DeactivatedScreen />
  }

  if (unprovisioned && !isMock) {
    return <PendingApprovalScreen />
  }

  if (meQ.data?.role === 'DAILY_STAFF') {
    if (!dailyStaffAllowed(location.pathname)) {
      return <Navigate to="/orders/new" replace />
    }
    return <OperatorLayout />
  }

  return <Layout />
}
