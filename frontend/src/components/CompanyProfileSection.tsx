import { useForm } from 'react-hook-form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useMe } from '../auth/useMe'
import type { CompanyProfile } from '../types/api'

const field =
  'w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 read-only:bg-gray-100 read-only:text-gray-500'
const label = 'block text-xs font-medium text-gray-600 mb-1'

/**
 * The company letterhead printed on every invoice and monthly billing PDF. A single editable
 * record. SUPER_ADMIN may edit (this lives on the SUPER_ADMIN-only Master Data screen). Changing it
 * updates DRAFT billings on the next render but never rewrites an already-issued/paid document (the
 * backend freezes a snapshot at issue time).
 *
 * <p>Bank-transfer details are not here — they are per-client, managed in BankAccountsSection.
 */
export default function CompanyProfileSection() {
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const canEdit = useMe().data?.role === 'SUPER_ADMIN'

  const { data, isLoading, error } = useQuery({
    queryKey: ['company-profile'],
    queryFn: async () =>
      apiFetch<CompanyProfile>('/api/company-profile', { token: await getAccessTokenSilently() }),
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<CompanyProfile>({ values: data })

  const mutation = useMutation({
    mutationFn: async (v: CompanyProfile) =>
      apiFetch<CompanyProfile>('/api/company-profile', {
        method: 'PUT',
        token: await getAccessTokenSilently(),
        body: JSON.stringify(v),
      }),
    onSuccess: (saved) => {
      qc.setQueryData(['company-profile'], saved)
      reset(saved)
    },
  })

  return (
    <section>
      <div className="mb-3">
        <h2 className="text-base font-semibold text-gray-800">Profil Perusahaan</h2>
        <p className="text-xs text-gray-500">
          Kop surat yang tampil pada PDF invoice dan tagihan.
          {!canEdit && ' Hanya Admin Super yang dapat mengubah.'}
        </p>
      </div>

      {isLoading && <div className="text-sm text-gray-400">Memuat...</div>}
      {error && <div className="text-sm text-red-500">Gagal memuat profil perusahaan.</div>}

      {data && (
        <form
          onSubmit={handleSubmit((v) => mutation.mutate(v))}
          className="space-y-5 rounded-lg border bg-white p-5 shadow-sm"
        >
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className={label}>Nama Perusahaan</label>
              <input className={field} readOnly={!canEdit} {...register('companyName', { required: true })} />
              {errors.companyName && <p className="mt-1 text-xs text-red-600">Wajib diisi.</p>}
            </div>
            <div className="sm:col-span-2">
              <label className={label}>Alamat</label>
              <input className={field} readOnly={!canEdit} {...register('address', { required: true })} />
              {errors.address && <p className="mt-1 text-xs text-red-600">Wajib diisi.</p>}
            </div>
            <div>
              <label className={label}>No. Telepon / HP</label>
              <input className={field} readOnly={!canEdit} {...register('phone', { required: true })} />
              {errors.phone && <p className="mt-1 text-xs text-red-600">Wajib diisi.</p>}
            </div>
          </div>

          {mutation.isError && (
            <p className="text-sm text-red-600">
              {mutation.error instanceof ApiError ? mutation.error.detail : 'Gagal menyimpan.'}
            </p>
          )}

          {canEdit && (
            <div className="flex items-center justify-end gap-3">
              {mutation.isSuccess && !isDirty && (
                <span className="text-sm text-green-600">Tersimpan.</span>
              )}
              <button
                type="submit"
                disabled={!isDirty || mutation.isPending}
                className="rounded-md bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-50"
              >
                {mutation.isPending ? 'Menyimpan...' : 'Simpan'}
              </button>
            </div>
          )}
        </form>
      )}
    </section>
  )
}
