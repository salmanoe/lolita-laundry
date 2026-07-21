/**
 * Recovery from a wedged Auth0 session.
 *
 * Symptom this exists for: the app boots to a spinner that never resolves, and the only thing that
 * fixes it is the user clearing site data in their browser settings — which end users cannot be
 * expected to know how to do (and on mobile is buried several menus deep). This module lets the app
 * perform the same purge itself.
 *
 * Why a session gets wedged:
 *  - Refresh Token Rotation with reuse detection (enabled on our tenant) revokes the whole token
 *    family when a rotated token is replayed. Two tabs, a restored bfcache page, or an interrupted
 *    refresh can trigger that; the SDK then holds a refresh token the tenant has already killed.
 *  - iOS/Safari ITP caps scriptable storage lifetime and can evict entries, leaving a partial cache
 *    the SDK can neither use nor recover from.
 * In both cases `checkSession()` rejects, the SDK settles to not-authenticated with `error` set, and
 * the stale entry survives in localStorage across reloads — so reloading never helps.
 */

/** Key prefix auth0-spa-js uses for its localStorage cache (and the transaction/nonce entries). */
const AUTH0_KEY_PREFIXES = ['@@auth0spajs@@', 'a0.spajs.txs.']

/**
 * Drop every Auth0 SDK entry from local/session storage. Equivalent to the auth-relevant part of
 * "clear site data", but scoped: it never touches app state we own.
 */
export function purgeAuthCache(): void {
  for (const store of [window.localStorage, window.sessionStorage]) {
    try {
      const doomed: string[] = []
      for (let i = 0; i < store.length; i++) {
        const key = store.key(i)
        if (key && AUTH0_KEY_PREFIXES.some((p) => key.startsWith(p))) doomed.push(key)
      }
      doomed.forEach((key) => store.removeItem(key))
    } catch {
      // Private mode / storage disabled: nothing to purge, and throwing here would replace a
      // recoverable auth error with a hard crash.
    }
  }
}

/**
 * One-shot guard so automatic recovery can never become a redirect loop: if a purge+relogin already
 * happened in this tab and we are STILL failing, the problem is not a stale cache and we must stop
 * and show the user a real error instead of bouncing them to Auth0 forever.
 */
const RECOVERY_FLAG = 'lolita.authRecoveryAttempted'

export function recoveryAlreadyAttempted(): boolean {
  try {
    return window.sessionStorage.getItem(RECOVERY_FLAG) === '1'
  } catch {
    return false
  }
}

export function markRecoveryAttempted(): void {
  try {
    window.sessionStorage.setItem(RECOVERY_FLAG, '1')
  } catch {
    /* storage disabled — worst case we retry once more */
  }
}

/** Called once a session is successfully established, so a later failure gets its own retry. */
export function clearRecoveryFlag(): void {
  try {
    window.sessionStorage.removeItem(RECOVERY_FLAG)
  } catch {
    /* ignore */
  }
}
