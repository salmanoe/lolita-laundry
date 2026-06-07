import { useForm } from 'react-hook-form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import Modal from './Modal'
import { indexById, useLookupList } from '../lib/lookups'
import type { Department, Item } from '../types/api'

interface FormValues {
  itemId: number
  pricePerUnit: number
  effectiveDate: string
  departmentId: number // 0 = none (COMBINED clients)
}

interface Props {
  open: boolean
  onClose: () => void
  clientId: number
  /** PER_DEPARTMENT clients require an item→department mapping; others must not send one. */
  perDepartment: boolean
  /** Active departments to choose from (PER_DEPARTMENT only). */
  departments: Department[]
  presetItemId?: number // pre-select an item (e.g. "change price" on an existing row)
  presetDepartmentId?: number // pre-select the item's current department
  presetPrice?: number // pre-fill the current price (e.g. "Ubah Harga" on an existing row)
  presetEffectiveDate?: string // pre-fill the current effective date (ISO yyyy-MM-dd)
}

const field =
  'w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'
const label = 'block text-xs font-medium text-gray-600 mb-1'

export default function SetPriceModal({
  open, onClose, clientId, perDepartment, departments, presetItemId, presetDepartmentId, presetPrice,
  presetEffectiveDate,
}: Props) {
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()

  const { data: items } = useQuery({
    queryKey: ['items', 'options'],
    queryFn: async () => {
      const token = await getAccessTokenSilently()
      return apiFetch<Item[]>('/api/items/options', { token })
    },
  })

  const unitsById = indexById(useLookupList('item-units').data)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    values: {
      itemId: presetItemId ?? 0,
      pricePerUnit: presetPrice ?? 0,
      effectiveDate: presetEffectiveDate ?? '',
      departmentId: presetDepartmentId ?? 0,
    },
  })

  const mutation = useMutation({
    mutationFn: async (v: FormValues) => {
      const token = await getAccessTokenSilently()
      return apiFetch(`/api/clients/${clientId}/prices`, {
        method: 'POST',
        token,
        body: JSON.stringify({
          itemId: Number(v.itemId),
          pricePerUnit: Number(v.pricePerUnit),
          effectiveDate: v.effectiveDate || null, // null → server defaults to today
          // departmentId required for PER_DEPARTMENT, must be null otherwise
          departmentId: perDepartment ? Number(v.departmentId) : null,
        }),
      })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['prices', clientId] })
      onClose()
    },
  })

  const activeItems = items?.filter((i) => i.active) ?? []

  return (
    <Modal open={open} title="Atur Harga" onClose={onClose}>
      <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-3">
        <div>
          <label className={label}>Item</label>
          <select
            className={field}
            disabled={presetItemId != null}
            {...register('itemId', { required: true, min: 1 })}
          >
            <option value={0} disabled>
              — Pilih item —
            </option>
            {activeItems.map((i) => (
              <option key={i.id} value={i.id}>
                {i.name} ({unitsById.get(i.unitId)?.displayName ?? '—'})
              </option>
            ))}
          </select>
          {errors.itemId && <p className="mt-1 text-xs text-red-600">Pilih item terlebih dahulu.</p>}
        </div>

        {perDepartment && (
          <div>
            <label className={label}>Departemen</label>
            <select className={field} {...register('departmentId', { required: true, min: 1, valueAsNumber: true })}>
              <option value={0} disabled>— Pilih departemen —</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
            {errors.departmentId && <p className="mt-1 text-xs text-red-600">Pilih departemen item ini.</p>}
            <p className="mt-1 text-xs text-gray-400">Menentukan tagihan departemen mana item ini masuk.</p>
          </div>
        )}

        <div>
          <label className={label}>Harga per Satuan (Rp)</label>
          <input
            type="number"
            min={0}
            step="any"
            className={field}
            {...register('pricePerUnit', { required: true, min: 0, valueAsNumber: true })}
          />
          {errors.pricePerUnit && (
            <p className="mt-1 text-xs text-red-600">Harga tidak boleh negatif.</p>
          )}
        </div>

        <div>
          <label className={label}>Berlaku Mulai (opsional)</label>
          <input type="date" className={field} {...register('effectiveDate')} />
          <p className="mt-1 text-xs text-gray-400">
            Kosongkan untuk berlaku hari ini. Harga lama tetap tersimpan (riwayat).
          </p>
        </div>

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
