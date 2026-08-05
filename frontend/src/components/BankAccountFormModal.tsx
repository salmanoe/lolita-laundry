import { useForm } from 'react-hook-form'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import Modal from './Modal'
import { useBankAccounts } from '../lib/bankAccounts'
import type { BankAccount } from '../types/api'

interface FormValues {
  label: string
  beneficiary: string
  bankName: string
  accountNumber: string
  accountHolder: string
}

interface Props {
  open: boolean
  onClose: () => void
  entry?: BankAccount // undefined → create mode
}

const field =
  'w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'
const label = 'block text-xs font-medium text-gray-600 mb-1'

/**
 * Create/edit one bank account. The default flag and the active flag are not edited here — both
 * have cross-row rules (exactly one default; the default can't be deactivated) and their own
 * actions in the list.
 */
export default function BankAccountFormModal({ open, onClose, entry }: Props) {
  const editing = !!entry
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const { data: list } = useBankAccounts()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    values: {
      label: entry?.label ?? '',
      beneficiary: entry?.beneficiary ?? '',
      bankName: entry?.bankName ?? '',
      accountNumber: entry?.accountNumber ?? '',
      accountHolder: entry?.accountHolder ?? '',
    },
  })

  const mutation = useMutation({
    mutationFn: async (v: FormValues) => {
      const token = await getAccessTokenSilently()
      // Preserve the row's place on edit; append to the end on create.
      const sortOrder = editing
        ? entry!.sortOrder
        : Math.max(0, ...(list ?? []).map((a) => a.sortOrder)) + 1
      return apiFetch(editing ? `/api/bank-accounts/${entry!.id}` : '/api/bank-accounts', {
        method: editing ? 'PUT' : 'POST',
        token,
        body: JSON.stringify({ ...v, sortOrder }),
      })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bank-accounts'] })
      qc.invalidateQueries({ queryKey: ['bank-account-options'] })
      onClose()
    },
  })

  return (
    <Modal open={open} title={editing ? 'Ubah Rekening' : 'Tambah Rekening'} onClose={onClose}>
      <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-3">
        <div>
          <label className={label}>Nama Rekening</label>
          <input className={field} placeholder="Rekening Perusahaan" {...register('label', { required: true })} />
          <p className="mt-1 text-xs text-gray-400">
            Label internal untuk memilih rekening ini pada klien. Tidak tercetak di PDF.
          </p>
          {errors.label && <p className="mt-1 text-xs text-red-600">Wajib diisi.</p>}
        </div>

        <div className="border-t pt-3">
          <h3 className="mb-3 text-xs font-semibold uppercase tracking-wide text-gray-500">
            Detail Transfer
          </h3>
          <div className="space-y-3">
            <div>
              <label className={label}>Penerima</label>
              <input className={field} {...register('beneficiary', { required: true })} />
              {errors.beneficiary && <p className="mt-1 text-xs text-red-600">Wajib diisi.</p>}
            </div>
            <div>
              <label className={label}>Bank</label>
              <input className={field} placeholder="Bank BCA" {...register('bankName', { required: true })} />
              {errors.bankName && <p className="mt-1 text-xs text-red-600">Wajib diisi.</p>}
            </div>
            <div>
              <label className={label}>No. Rekening</label>
              <input className={field} {...register('accountNumber', { required: true })} />
              {errors.accountNumber && <p className="mt-1 text-xs text-red-600">Wajib diisi.</p>}
            </div>
            <div>
              <label className={label}>Nama Pemilik Rekening</label>
              <input className={field} {...register('accountHolder', { required: true })} />
              {errors.accountHolder && <p className="mt-1 text-xs text-red-600">Wajib diisi.</p>}
            </div>
          </div>
        </div>

        {mutation.isError && (
          <p className="text-sm text-red-600">
            {mutation.error instanceof ApiError ? mutation.error.detail : 'Gagal menyimpan.'}
          </p>
        )}

        <div className="flex justify-end gap-2 pt-1">
          <button type="button" onClick={onClose} className="rounded-md px-4 py-2 text-sm text-gray-600 hover:bg-gray-100">
            Batal
          </button>
          <button
            type="submit"
            disabled={mutation.isPending}
            className="rounded-md bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-50"
          >
            {mutation.isPending ? 'Menyimpan...' : 'Simpan'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
