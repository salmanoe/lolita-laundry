import { useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../auth/AuthContext'

/**
 * Shown to an authenticated user whose Auth0 identity is not yet provisioned (GET /api/me → 204).
 * On first login {@link RoleLayout} has already self-registered them into the pending queue; this
 * screen tells them to wait for a SUPER_ADMIN to approve and assign a role. "Periksa Lagi" re-checks
 * /api/me (after approval it resolves to a role and the app routes them in).
 */
export default function PendingApprovalScreen() {
  const { logout, user } = useAuth()
  const qc = useQueryClient()

  return (
    <div className="flex h-screen flex-col items-center justify-center bg-gradient-to-br from-brand-900 to-brand-600 px-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-10 text-center shadow-xl">
        <div className="mx-auto mb-5 flex h-14 w-14 items-center justify-center rounded-full bg-amber-100">
          <svg className="h-7 w-7 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6l4 2m6-2a10 10 0 11-20 0 10 10 0 0120 0z" />
          </svg>
        </div>
        <h1 className="mb-2 text-xl font-bold text-gray-800">Menunggu Persetujuan</h1>
        <p className="mb-1 text-sm text-gray-600">
          Akun Anda{user?.email ? ` (${user.email})` : ''} sudah terdaftar dan menunggu persetujuan administrator.
        </p>
        <p className="mb-6 text-sm text-gray-500">
          Anda dapat mengakses sistem setelah administrator menyetujui dan menetapkan peran Anda.
        </p>
        <div className="flex flex-col gap-2">
          <button
            onClick={() => qc.invalidateQueries({ queryKey: ['me'] })}
            className="w-full rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-500"
          >
            Periksa Lagi
          </button>
          <button
            onClick={() => logout({ logoutParams: { returnTo: window.location.origin, federated: true } })}
            className="text-sm font-medium text-gray-500 hover:text-gray-700"
          >
            Keluar
          </button>
        </div>
      </div>
    </div>
  )
}
