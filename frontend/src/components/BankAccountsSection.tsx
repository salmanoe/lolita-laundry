import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import BankAccountFormModal from './BankAccountFormModal'
import { useBankAccounts } from '../lib/bankAccounts'
import { useMe } from '../auth/useMe'
import type { BankAccount } from '../types/api'

/**
 * The company's bank accounts, printed in the transfer block of monthly billing PDFs. Each client
 * is billed to one of these; the default account covers every client with no explicit assignment
 * (set per client on the Klien screen).
 *
 * Changing an account updates DRAFT billings on the next render but never rewrites an issued or
 * paid invoice — the backend freezes the account onto the document at issue time.
 */
export default function BankAccountsSection() {
  const { data, isLoading, error } = useBankAccounts()
  const [form, setForm] = useState<{ open: boolean; entry?: BankAccount }>({ open: false })
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  // Mirrors CompanyProfileSection: the route is already SUPER_ADMIN-only and every mutation is
  // SUPER_ADMIN server-side, so this is defence in depth — it keeps the controls from being
  // offered to someone who would only get a 403 back.
  const canEdit = useMe().data?.role === 'SUPER_ADMIN'

  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['bank-accounts'] })
    qc.invalidateQueries({ queryKey: ['bank-account-options'] })
  }

  const setDefault = useMutation({
    mutationFn: async (id: number) =>
      apiFetch(`/api/bank-accounts/${id}/default`, {
        method: 'POST',
        token: await getAccessTokenSilently(),
      }),
    onSuccess: refresh,
  })

  const setActive = useMutation({
    mutationFn: async ({ id, active }: { id: number; active: boolean }) =>
      apiFetch(`/api/bank-accounts/${id}/status`, {
        method: 'PATCH',
        token: await getAccessTokenSilently(),
        body: JSON.stringify({ active }),
      }),
    onSuccess: refresh,
  })

  const pending = setDefault.isPending || setActive.isPending
  const actionError = setDefault.error ?? setActive.error

  return (
    <section>
      <div className="mb-3 flex items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold text-gray-800">Rekening Bank</h2>
          <p className="text-xs text-gray-500">
            Rekening tujuan transfer pada PDF tagihan bulanan. Klien tanpa pilihan khusus memakai
            rekening default.
            {!canEdit && ' Hanya Admin Super yang dapat mengubah.'}
          </p>
        </div>
        {canEdit && (
          <button
            onClick={() => setForm({ open: true })}
            className="whitespace-nowrap rounded-lg bg-brand-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-700"
          >
            + Tambah Rekening
          </button>
        )}
      </div>

      {isLoading && <div className="text-sm text-gray-400">Memuat...</div>}
      {error && <div className="text-sm text-red-500">Gagal memuat rekening bank.</div>}

      {actionError && (
        <div className="mb-3 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          {actionError instanceof ApiError ? actionError.detail : 'Gagal memperbarui rekening.'}
        </div>
      )}

      {data && (
        <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['Nama Rekening', 'Bank', 'No. Rekening', 'Pemilik', 'Status', ''].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {data.map((row) => (
                <tr key={row.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <span className="font-medium text-gray-800">{row.label}</span>
                    {row.defaultAccount && (
                      <span className="ml-2 inline-flex rounded-full bg-brand-100 px-2 py-0.5 text-xs font-medium text-brand-700">
                        Default
                      </span>
                    )}
                    <div className="text-xs text-gray-400">{row.beneficiary}</div>
                  </td>
                  <td className="px-4 py-3 text-gray-700">{row.bankName}</td>
                  <td className="px-4 py-3 font-mono text-gray-700">{row.accountNumber}</td>
                  <td className="px-4 py-3 text-gray-700">{row.accountHolder}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                      row.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                    }`}>
                      {row.active ? 'Aktif' : 'Nonaktif'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex justify-end gap-3 whitespace-nowrap">
                      {canEdit && !row.defaultAccount && row.active && (
                        <button
                          disabled={pending}
                          onClick={() => setDefault.mutate(row.id)}
                          className="text-sm font-medium text-gray-500 hover:text-gray-700 disabled:opacity-50"
                        >
                          Jadikan Default
                        </button>
                      )}
                      {canEdit && !row.defaultAccount && (
                        <button
                          disabled={pending}
                          onClick={() => setActive.mutate({ id: row.id, active: !row.active })}
                          className="text-sm font-medium text-gray-500 hover:text-gray-700 disabled:opacity-50"
                        >
                          {row.active ? 'Nonaktifkan' : 'Aktifkan'}
                        </button>
                      )}
                      {canEdit && (
                        <button
                          onClick={() => setForm({ open: true, entry: row })}
                          className="text-sm font-medium text-brand-600 hover:text-brand-700"
                        >
                          Ubah
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {data.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-4 py-6 text-center text-sm text-gray-400">
                    Belum ada rekening.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      <BankAccountFormModal
        open={form.open}
        entry={form.entry}
        onClose={() => setForm({ open: false })}
      />
    </section>
  )
}
