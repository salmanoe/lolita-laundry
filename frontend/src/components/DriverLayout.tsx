import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useMe } from '../auth/useMe'

/**
 * Minimal shell for the DRIVER role: navy→ocean gradient header with the white logo and a
 * logout button, no admin sidebar. Drivers only ever see their delivery screen.
 *
 * Guards the route: a non-driver who lands here is bounced to the admin app. While `me` is
 * still loading we render nothing to avoid a flash of the wrong layout.
 */
export default function DriverLayout() {
  const { user, logout } = useAuth()
  const meQ = useMe()

  if (meQ.isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-gray-50">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
      </div>
    )
  }

  // me === null (dev/unprovisioned) or a non-driver role → not a driver: send to the admin app.
  if (meQ.data?.role !== 'DRIVER') {
    return <Navigate to="/" replace />
  }

  return (
    <div className="flex min-h-screen flex-col bg-gray-50">
      <header className="flex items-center justify-between bg-gradient-to-r from-brand-800 via-brand-700 to-brand-500 px-4 py-3 text-white shadow-sm">
        <div className="flex items-center gap-3">
          <img src="/logo-white.png" alt="Logo Lolita Laundry" className="h-9 w-auto" />
          <span className="text-sm font-semibold">Pengiriman</span>
        </div>
        <div className="flex items-center gap-3">
          <span className="hidden text-xs text-blue-100/80 sm:inline">{meQ.data.fullName ?? user?.name}</span>
          <button
            onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}
            className="rounded-lg bg-white/15 px-3 py-1.5 text-xs font-medium text-white hover:bg-white/25"
          >
            Keluar
          </button>
        </div>
      </header>

      <main className="mx-auto w-full max-w-2xl flex-1 p-4">
        <Outlet />
      </main>
    </div>
  )
}
