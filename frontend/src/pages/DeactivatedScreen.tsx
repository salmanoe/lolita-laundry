import { useAuth } from '../auth/AuthContext'

/**
 * Terminal screen for a provisioned user whose account has been deactivated by a SUPER_ADMIN
 * (GET /api/me → 403 "account_deactivated"). Their Auth0 session is still technically valid — the
 * backend cut them off, not Auth0 — so there is nothing to retry: the only action is to log out.
 * {@link RoleLayout} renders this in place of the app shell when it detects that 403.
 */
export default function DeactivatedScreen() {
  const { logout, user } = useAuth()

  return (
    <div className="flex h-screen flex-col items-center justify-center bg-gradient-to-br from-brand-900 to-brand-600 px-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-10 text-center shadow-xl">
        <div className="mx-auto mb-5 flex h-14 w-14 items-center justify-center rounded-full bg-red-100">
          <svg className="h-7 w-7 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
          </svg>
        </div>
        <h1 className="mb-2 text-xl font-bold text-gray-800">Akun Dinonaktifkan</h1>
        <p className="mb-1 text-sm text-gray-600">
          Akun Anda{user?.email ? ` (${user.email})` : ''} telah dinonaktifkan oleh administrator.
        </p>
        <p className="mb-6 text-sm text-gray-500">
          Anda tidak lagi memiliki akses ke sistem. Hubungi administrator jika menurut Anda ini keliru.
        </p>
        <button
          onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}
          className="w-full rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-500"
        >
          Keluar
        </button>
      </div>
    </div>
  )
}
