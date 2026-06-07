import { useForm } from 'react-hook-form'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import Modal from './Modal'
import { useLookupList } from '../lib/lookups'
import type { Item } from '../types/api'

interface FormValues {
  name: string
  unitId: number
  active: boolean
}

interface Props {
  open: boolean
  onClose: () => void
  item?: Item // undefined → create mode
}

const field =
  'w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'
const label = 'block text-xs font-medium text-gray-600 mb-1'

export default function ItemFormModal({ open, onClose, item }: Props) {
  const editing = !!item
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()

  const units = useLookupList('item-units')
  // Active options, plus the item's current value even if it was since deactivated.
  const unitOptions = (units.data ?? []).filter((u) => u.active || u.id === item?.unitId)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    values: {
      name: item?.name ?? '',
      unitId: item?.unitId ?? 0,
      active: item?.active ?? true,
    },
  })

  const mutation = useMutation({
    mutationFn: async (v: FormValues) => {
      const token = await getAccessTokenSilently()
      if (editing) {
        return apiFetch(`/api/items/${item!.id}`, {
          method: 'PUT',
          token,
          body: JSON.stringify({ name: v.name, unitId: v.unitId, active: v.active }),
        })
      }
      return apiFetch('/api/items', {
        method: 'POST',
        token,
        body: JSON.stringify({ name: v.name, unitId: v.unitId }),
      })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['items'] })
      onClose()
    },
  })

  return (
    <Modal open={open} title={editing ? 'Ubah Item' : 'Tambah Item'} onClose={onClose}>
      <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-3">
        <div>
          <label className={label}>Nama</label>
          <input className={field} {...register('name', { required: true })} />
          {errors.name && <p className="mt-1 text-xs text-red-600">Nama wajib diisi.</p>}
        </div>

        <div>
          <label className={label}>Satuan</label>
          <select className={field} {...register('unitId', { required: true, min: 1, valueAsNumber: true })}>
            <option value={0} disabled>— Pilih —</option>
            {unitOptions.map((u) => (
              <option key={u.id} value={u.id}>{u.displayName}</option>
            ))}
          </select>
          {errors.unitId && <p className="mt-1 text-xs text-red-600">Pilih satuan.</p>}
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