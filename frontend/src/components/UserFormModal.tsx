import { useForm } from 'react-hook-form'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import Modal from './Modal'
import { roleLabel } from '../lib/labels'
import type { Role, User } from '../types/api'

interface FormValues {
  auth0Sub: string
  fullName: string
  role: Role
}

interface Props {
  open: boolean
  onClose: () => void
  entry?: User // undefined → create mode
}

const ROLES: Role[] = ['SUPER_ADMIN', 'FINANCE_STAFF', 'DAILY_STAFF']

const field =
  'w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'
const label = 'block text-xs font-medium text-gray-600 mb-1'

export default function UserFormModal({ open, onClose, entry }: Props) {
  const editing = !!entry
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    values: {
      auth0Sub: entry?.auth0Sub ?? '',
      fullName: entry?.fullName ?? '',
      role: entry?.role ?? 'DAILY_STAFF',
    },
  })

  const mutation = useMutation({
    mutationFn: async (v: FormValues) => {
      const token = await getAccessTokenSilently()
      if (editing) {
        return apiFetch(`/api/users/${entry!.id}`, {
          method: 'PUT',
          token,
          body: JSON.stringify({ fullName: v.fullName, role: v.role }),
        })
      }
      return apiFetch('/api/users', {
        method: 'POST',
        token,
        body: JSON.stringify({ auth0Sub: v.auth0Sub.trim(), fullName: v.fullName, role: v.role }),
      })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] })
      onClose()
    },
  })

  return (
    <Modal open={open} title={editing ? 'Ubah Pengguna' : 'Tambah Pengguna'} onClose={onClose}>
      <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-3">
        <div>
          <label className={label}>Auth0 Sub</label>
          <input
            className={field + (editing ? ' bg-gray-100 text-gray-500' : ' font-mono')}
            readOnly={editing}
            placeholder="auth0|abc123"
            {...register('auth0Sub', { required: !editing })}
          />
          {editing ? (
            <p className="mt-1 text-xs text-gray-400">Identitas Auth0 tidak dapat diubah.</p>
          ) : errors.auth0Sub ? (
            <p className="mt-1 text-xs text-red-600">Auth0 sub wajib diisi.</p>
          ) : (
            <p className="mt-1 text-xs text-gray-400">
              Buat akun di dasbor Auth0 dulu, lalu tempel nilai <span className="font-mono">sub</span>-nya di sini.
            </p>
          )}
        </div>

        <div>
          <label className={label}>Nama Lengkap</label>
          <input className={field} placeholder="Budi Santoso" {...register('fullName', { required: true })} />
          {errors.fullName && <p className="mt-1 text-xs text-red-600">Nama wajib diisi.</p>}
        </div>

        <div>
          <label className={label}>Peran</label>
          <select className={field} {...register('role', { required: true })}>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {roleLabel[r]}
              </option>
            ))}
          </select>
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