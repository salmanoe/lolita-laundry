import { ReactNode, useCallback, useEffect, useState } from 'react'
import { useAuth } from './AuthContext'
import {
  clearRecoveryFlag,
  markRecoveryAttempted,
  purgeAuthCache,
  recoveryAlreadyAttempted,
} from './recovery'

interface Props {
  children: ReactNode
}

/**
 * How long the Auth0 SDK may stay in `isLoading` before we treat it as hung. `checkSession()` has no
 * timeout of its own, so on a flaky mobile connection — or when a silent-auth iframe is blocked and
 * never posts a message back — it can pend forever. Without this the user stares at a spinner with
 * no way out. Generous enough not to fire on a merely slow 3G handshake.
 */
const AUTH_TIMEOUT_MS = 20_000

export default function AuthGuard({ children }: Props) {
  const { isLoading, isAuthenticated, error, loginWithRedirect } = useAuth()
  const [timedOut, setTimedOut] = useState(false)

  // A wedged session is unrecoverable by reloading (the bad entry is persisted), so a plain retry
  // button is not enough — the cache has to go with it.
  const resetAndLogin = useCallback(() => {
    purgeAuthCache()
    markRecoveryAttempted()
    void loginWithRedirect().catch(() => {
      // The redirect itself failed (offline, popup/redirect blocked). Fall through to the error
      // screen rather than leaving an unhandled rejection and a frozen UI.
      setTimedOut(true)
    })
  }, [loginWithRedirect])

  // Watchdog: only armed while the SDK is still deciding.
  useEffect(() => {
    if (!isLoading) return
    const t = window.setTimeout(() => setTimedOut(true), AUTH_TIMEOUT_MS)
    return () => window.clearTimeout(t)
  }, [isLoading])

  useEffect(() => {
    if (isLoading || isAuthenticated) return

    // Session failed to establish. If this is the first failure in this tab, self-heal: purge the
    // (likely poisoned) Auth0 cache and send them through login again. This is the automatic
    // equivalent of the manual "clear site data" workaround.
    if (error && !recoveryAlreadyAttempted()) {
      resetAndLogin()
      return
    }

    // No error, just no session — ordinary "not logged in yet" path.
    if (!error) {
      void loginWithRedirect().catch(() => setTimedOut(true))
    }
  }, [isLoading, isAuthenticated, error, loginWithRedirect, resetAndLogin])

  // Session is good — allow a future failure its own one-shot recovery.
  useEffect(() => {
    if (isAuthenticated) clearRecoveryFlag()
  }, [isAuthenticated])

  if (isAuthenticated) return <>{children}</>

  // Dead end: either the SDK hung past the watchdog, or we already tried purge+relogin once and are
  // still failing. Show a real screen with a manual escape instead of an eternal spinner.
  const stuck = timedOut || (error && recoveryAlreadyAttempted())
  if (stuck) {
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-4 bg-gray-50 px-6 text-center">
        <h1 className="text-lg font-semibold text-gray-800">Gagal masuk</h1>
        <p className="max-w-sm text-sm text-gray-500">
          Sesi Anda bermasalah atau koneksi terputus. Tekan tombol di bawah untuk membersihkan sesi
          dan masuk ulang.
        </p>
        <button
          type="button"
          onClick={() => {
            setTimedOut(false)
            purgeAuthCache()
            void loginWithRedirect().catch(() => setTimedOut(true))
          }}
          className="rounded-lg bg-brand-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-brand-700"
        >
          Masuk Ulang
        </button>
      </div>
    )
  }

  return (
    <div className="flex h-screen items-center justify-center">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
    </div>
  )
}
