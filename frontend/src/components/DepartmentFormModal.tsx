import { useForm } from 'react-hook-form'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import Modal from './Modal'
import type { Department } from '../types/api'

interface FormValues {
  name: string
  active: boolean
}

interface Props {
  open: boolean
  onClose: () => void
  clientId: number
  department?: Department // undefined → create mode
}

const field =
  'w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'
const label = 'block text-xs font-medium text-gray-600 mb-1'

export default function DepartmentFormModal({ open, onClose, clientId, department }: Props) {
  const editing = !!department
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    values: {
      name: department?.name ?? '',
      active: department?.active ?? true,
    },
  })

  const mutation = useMutation({
    mutationFn: async (v: FormValues) => {
      const token = await getAccessTokenSilently()
      // DepartmentRequest requires `active` on create too (server forces true on create).
      const body = JSON.stringify({ name: v.name, active: v.active })
      if (editing) {
        return apiFetch(`/api/clients/${clientId}/departments/${department!.id}`, {
          method: 'PUT',
          token,
          body,
        })
      }
      return apiFetch(`/api/clients/${clientId}/departments`, { method: 'POST', token, body })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['departments', clientId] })
      onClose()
    },
  })

  return (
    <Modal open={open} title={editing ? 'Ubah Departemen' : 'Tambah Departemen'} onClose={onClose}>
      <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-3">
        <div>
          <label className={label}>Nama Departemen</label>
          <input className={field} placeholder="Room Linen" {...register('name', { required: true })} />
          {errors.name && <p className="mt-1 text-xs text-red-600">Nama wajib diisi.</p>}
        </div>

        {editing && (
          <label className="flex items-center gap-2 text-sm text-gray-700">
            <input type="checkbox" {...register('active')} className="rounded border-gray-300" />
            Aktif
          </label>
        )}

        {mutation.isError && (
          <p className="text-sm text-red-600">
            {mutation.error instanceof ApiError ? mutation.error.detail : 'Gagal menyimpan.'}
          </p>
        )}

        <div className="flex justify-end gap-2 pt-1">
          <button
            type="button"
            onClick={onClose}
            className="rounded-md px-4 py-2 text-sm text-gray-600 hover:bg-gray-100"
          >
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
