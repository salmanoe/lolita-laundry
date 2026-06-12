import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useMe } from '../auth/useMe'

/**
 * Minimal shell for the DAILY_STAFF operator role: a navy→ocean gradient header with the white
 * logo + logout, and a 3-tab nav — Buat Order, Order, Pengantaran. No admin sidebar. Mobile-first
 * (these are in-house staff working on a phone). Route gating (keeping operators on these three
 * screens) is handled by RoleLayout; this is purely the chrome.
 */
const navItems = [
  { to: '/orders/new', label: 'Buat Order', Icon: PlusIcon },
  { to: '/orders', label: 'Order', Icon: ListIcon },
  { to: '/deliveries', label: 'Pengantaran', Icon: TruckIcon },
]

export default function OperatorLayout() {
  const { user, logout } = useAuth()
  const meQ = useMe()

  return (
    <div className="flex min-h-screen flex-col bg-gray-50">
      <header className="bg-gradient-to-r from-brand-800 via-brand-700 to-brand-500 text-white shadow-sm">
        <div className="mx-auto flex w-full max-w-2xl items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            <img src="/logo-white.png" alt="Logo Lolita Laundry" className="h-9 w-auto" />
            <span className="text-sm font-semibold">Staf Harian</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="hidden text-xs text-blue-100/80 sm:inline">{meQ.data?.fullName ?? user?.name}</span>
            <button
              onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}
              className="rounded-lg bg-white/15 px-3 py-1.5 text-xs font-medium text-white hover:bg-white/25"
            >
              Keluar
            </button>
          </div>
        </div>

        {/* Tab nav */}
        <nav className="mx-auto flex w-full max-w-2xl gap-1 px-2 pb-1">
          {navItems.map(({ to, label, Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/orders'}
              className={({ isActive }) =>
                `flex flex-1 items-center justify-center gap-2 rounded-t-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                  isActive ? 'bg-gray-50 text-brand-700' : 'text-blue-100/90 hover:bg-white/10'
                }`
              }
            >
              <Icon className="h-[18px] w-[18px]" />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
      </header>

      <main className="mx-auto w-full max-w-2xl flex-1 p-4">
        <Outlet />
      </main>
    </div>
  )
}

function PlusIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <path d="M12 5v14M5 12h14" />
    </svg>
  )
}

function ListIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" />
    </svg>
  )
}

function TruckIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M1 3h15v13H1zM16 8h4l3 3v5h-7zM5.5 19a1.5 1.5 0 100-3 1.5 1.5 0 000 3zM18.5 19a1.5 1.5 0 100-3 1.5 1.5 0 000 3z" />
    </svg>
  )
}
