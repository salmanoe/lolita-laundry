import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useMe } from './useMe'

/**
 * Guards SUPER_ADMIN-only screens (Pengguna, Item, Master Data). A known FINANCE_STAFF/DAILY_STAFF is
 * bounced to the dashboard; an unresolved role (dev/mock — backend replies 204 to /api/me) is
 * allowed through, matching how the rest of the app fails open when there is no users row and
 * method security is disabled in the dev profile. The backend @PreAuthorize is the real enforcement.
 */
export default function RequireSuperAdmin({ children }: { children: ReactNode }) {
  const meQ = useMe()
  if (meQ.isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
      </div>
    )
  }
  const role = meQ.data?.role
  if (role === 'FINANCE_STAFF' || role === 'DAILY_STAFF') {
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}