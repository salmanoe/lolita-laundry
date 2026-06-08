import { useEffect, useRef, useState } from 'react'
import { Navigate, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useMe } from '../auth/useMe'
import { BasketIcon, ChartIcon, HomeIcon, HotelIcon, InvoiceIcon, SlidersIcon, TowelsIcon, UsersIcon } from './NavIcons'

// `ownerOnly` items are hidden from STAFF (the backend also enforces OWNER on /api/users).
const navItems = [
  { to: '/',        label: 'Dasbor', Icon: HomeIcon },
  { to: '/clients', label: 'Klien',  Icon: HotelIcon },
  { to: '/orders',  label: 'Order',  Icon: BasketIcon },
  { to: '/billing', label: 'Tagihan', Icon: InvoiceIcon },
  { to: '/reports', label: 'Laporan', Icon: ChartIcon },
  { to: '/items',   label: 'Item',   Icon: TowelsIcon, ownerOnly: true },
  { to: '/master-data', label: 'Master Data', Icon: SlidersIcon, ownerOnly: true },
  { to: '/users',   label: 'Pengguna', Icon: UsersIcon, ownerOnly: true },
]

function initials(name?: string) {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/)
  return (parts[0][0] + (parts[1]?.[0] ?? '')).toUpperCase()
}

export default function Layout() {
  const { user, logout } = useAuth()
  const meQ = useMe()
  const [menuOpen, setMenuOpen] = useState(false)
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  // Close the profile dropdown on any outside click (works inside the transformed drawer).
  useEffect(() => {
    if (!menuOpen) return
    function onDown(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', onDown)
    return () => document.removeEventListener('mousedown', onDown)
  }, [menuOpen])

  // Drivers have no admin access — send them to their delivery screen.
  if (meQ.data?.role === 'DRIVER') {
    return <Navigate to="/deliveries" replace />
  }

  const isOwner = meQ.data?.role === 'OWNER'
  const visibleNav = navItems.filter((item) => !item.ownerOnly || isOwner)

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Mobile drawer backdrop */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-30 bg-black/40 md:hidden" onClick={() => setSidebarOpen(false)} />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-gradient-to-b from-brand-800 via-brand-700 to-brand-500 text-white shadow-sm transition-transform duration-200 md:static md:z-auto md:w-56 md:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <NavLink
          to="/"
          onClick={() => setSidebarOpen(false)}
          className="flex items-center justify-center px-4 pt-6 pb-4 transition-opacity hover:opacity-90"
        >
          <img src="/logo-white.png" alt="Logo Lolita Laundry" className="h-24 w-auto" />
        </NavLink>

        {/* Profile (top) — click to reveal the Keluar menu */}
        <div ref={menuRef} className="relative border-b border-white/10 px-3 py-3">
          <button
            onClick={() => setMenuOpen((o) => !o)}
            className="flex w-full items-center gap-2 rounded-md px-2 py-2 text-left hover:bg-white/10"
          >
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-white/15 text-sm font-semibold text-white">
              {initials(user?.name)}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-xs font-medium text-white">{user?.name}</p>
              <p className="truncate text-xs text-blue-200/70">{user?.email}</p>
            </div>
            <span className={`text-white/60 transition-transform ${menuOpen ? 'rotate-180' : ''}`}>▾</span>
          </button>

          {menuOpen && (
            <div className="absolute left-3 right-3 z-20 mt-1 overflow-hidden rounded-md border bg-white py-1 shadow-lg">
              <button
                onClick={() => {
                  setMenuOpen(false)
                  logout({ logoutParams: { returnTo: window.location.origin } })
                }}
                className="block w-full px-3 py-2 text-left text-sm text-gray-600 hover:bg-gray-50 hover:text-red-600"
              >
                Keluar
              </button>
            </div>
          )}
        </div>

        <nav className="flex-1 px-3 py-4">
          <p className="px-2 pb-2 text-[10px] font-semibold uppercase tracking-wider text-blue-200/60">Menu</p>
          <div className="space-y-1">
            {visibleNav.map(({ to, label, Icon }) => (
              <NavLink
                key={to}
                to={to}
                end={to === '/'}
                onClick={() => setSidebarOpen(false)}
                className={({ isActive }) =>
                  `group flex items-center gap-3 rounded-xl px-2.5 py-2.5 text-sm font-medium transition-all ${
                    isActive
                      ? 'bg-white/15 text-white shadow-sm'
                      : 'text-blue-100/80 hover:bg-white/10 hover:text-white'
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    <span
                      className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-lg transition-colors ${
                        isActive ? 'bg-white/20' : 'bg-white/10 group-hover:bg-white/20'
                      }`}
                    >
                      <Icon className="h-[18px] w-[18px]" />
                    </span>
                    <span>{label}</span>
                    {isActive && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-brand-300" />}
                  </>
                )}
              </NavLink>
            ))}
          </div>
        </nav>
      </aside>

      {/* Main column */}
      <div className="flex min-w-0 flex-1 flex-col">
        {/* Mobile top bar */}
        <header className="flex h-14 items-center gap-3 border-b bg-white px-4 md:hidden">
          <button
            onClick={() => setSidebarOpen(true)}
            className="rounded-md p-1 text-2xl leading-none text-gray-600 hover:bg-gray-100"
            aria-label="Buka menu"
          >
            ☰
          </button>
          <img src="/favicon.png" alt="" className="h-7 w-7 rounded" />
          <span className="font-bold text-brand-700">Lolita Laundry</span>
        </header>

        <main className="flex-1 overflow-y-auto p-4 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
