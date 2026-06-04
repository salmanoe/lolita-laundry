import { useForm } from 'react-hook-form'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import Modal from './Modal'
import { billingModeLabel } from '../lib/labels'
import { useLookupList } from '../lib/lookups'
import type { Client, BillingMode } from '../types/api'

interface FormValues {
  name: string
  clientCode: string
  clientTypeId: number
  billingMode: BillingMode
  contactPerson: string
  phone: string
  address: string
}

interface Props {
  open: boolean
  onClose: () => void
  client?: Client // undefined → create mode
}

const field =
  'w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'
const label = 'block text-xs font-medium text-gray-600 mb-1'

export default function ClientFormModal({ open, onClose, client }: Props) {
  const editing = !!client
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()

  const clientTypes = useLookupList('client-types')
  const typeOptions = (clientTypes.data ?? []).filter((t) => t.active || t.id === client?.clientTypeId)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    values: {
      name: client?.name ?? '',
      clientCode: client?.clientCode ?? '',
      clientTypeId: client?.clientTypeId ?? 0,
      billingMode: client?.billingMode ?? 'COMBINED',
      contactPerson: client?.contactPerson ?? '',
      phone: client?.phone ?? '',
      address: client?.address ?? '',
    },
  })

  const mutation = useMutation({
    mutationFn: async (v: FormValues) => {
      const token = await getAccessTokenSilently()
      const payload = {
        name: v.name,
        clientTypeId: v.clientTypeId,
        billingMode: v.billingMode,
        contactPerson: v.contactPerson || null,
        phone: v.phone || null,
        address: v.address || null,
      }
      if (editing) {
        return apiFetch(`/api/clients/${client!.id}`, {
          method: 'PUT',
          token,
          body: JSON.stringify(payload), // clientCode is immutable — not sent
        })
      }
      return apiFetch('/api/clients', {
        method: 'POST',
        token,
        body: JSON.stringify({ ...payload, clientCode: v.clientCode }),
      })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['clients'] })
      if (editing) qc.invalidateQueries({ queryKey: ['client', client!.id] }) // refresh detail-page card
      onClose()
    },
  })

  return (
    <Modal open={open} title={editing ? 'Ubah Klien' : 'Tambah Klien'} onClose={onClose}>
      <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-3">
        <div>
          <label className={label}>Nama</label>
          <input className={field} {...register('name', { required: true })} />
          {errors.name && <p className="mt-1 text-xs text-red-600">Nama wajib diisi.</p>}
        </div>

        <div>
          <label className={label}>Kode Klien</label>
          <input
            className={field + (editing ? ' bg-gray-100 text-gray-500' : '')}
            readOnly={editing}
            placeholder="PBS"
            {...register('clientCode', { required: !editing, pattern: /^[A-Z0-9]+$/ })}
          />
          {editing ? (
            <p className="mt-1 text-xs text-gray-400">Kode tidak dapat diubah.</p>
          ) : (
            errors.clientCode && (
              <p className="mt-1 text-xs text-red-600">Huruf kapital/angka saja (mis. PBS).</p>
            )
          )}
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className={label}>Tipe</label>
            <select className={field} {...register('clientTypeId', { required: true, min: 1, valueAsNumber: true })}>
              <option value={0} disabled>— Pilih —</option>
              {typeOptions.map((t) => (
                <option key={t.id} value={t.id}>{t.displayName}</option>
              ))}
            </select>
            {errors.clientTypeId && <p className="mt-1 text-xs text-red-600">Pilih tipe.</p>}
          </div>
          <div>
            <label className={label}>Penagihan</label>
            <select className={field} {...register('billingMode')}>
              {(Object.entries(billingModeLabel) as [BillingMode, string][]).map(([v, l]) => (
                <option key={v} value={v}>{l}</option>
              ))}
            </select>
          </div>
        </div>

        <div>
          <label className={label}>Kontak (opsional)</label>
          <input className={field} {...register('contactPerson')} />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className={label}>Telepon (opsional)</label>
            <input className={field} {...register('phone')} />
          </div>
        </div>

        <div>
          <label className={label}>Alamat (opsional)</label>
          <textarea className={field} rows={2} {...register('address')} />
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
