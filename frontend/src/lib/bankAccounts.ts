import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { BankAccount, BankAccountOption } from '../types/api'

/**
 * Full bank-account records including transfer details — SUPER_ADMIN only, for the Master Data
 * editor. Mutations invalidate ['bank-accounts'] and ['bank-account-options'].
 */
export function useBankAccounts() {
  const { getAccessTokenSilently } = useAuth()
  return useQuery({
    queryKey: ['bank-accounts'],
    queryFn: async () =>
      apiFetch<BankAccount[]>('/api/bank-accounts', { token: await getAccessTokenSilently() }),
  })
}

/**
 * Labels only — readable by FINANCE_STAFF too, so the client screens can render and pick a
 * client's account. Includes inactive accounts so an existing assignment still shows a name;
 * filter on `.active` when building a picker.
 */
export function useBankAccountOptions() {
  const { getAccessTokenSilently } = useAuth()
  return useQuery({
    queryKey: ['bank-account-options'],
    queryFn: async () =>
      apiFetch<BankAccountOption[]>('/api/bank-accounts/options', {
        token: await getAccessTokenSilently(),
      }),
  })
}

/**
 * How a client's bank assignment reads on screen. A null assignment is not blank — it means the
 * client bills to whichever account is currently the default, so name that account.
 */
export function bankAccountLabel(
  bankAccountId: number | null | undefined,
  options: BankAccountOption[] | undefined,
): string {
  const list = options ?? []
  if (bankAccountId == null) {
    const fallback = list.find((a) => a.defaultAccount)
    return fallback ? `Default (${fallback.label})` : 'Rekening default'
  }
  const assigned = list.find((a) => a.id === bankAccountId)
  if (!assigned) return 'Rekening tidak dikenal'
  return assigned.active ? assigned.label : `${assigned.label} (nonaktif)`
}
