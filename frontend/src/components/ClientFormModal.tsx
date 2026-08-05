import { useForm } from 'react-hook-form'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import Modal from './Modal'
import ModalFormActions from './ModalFormActions'
import { billingModeLabel } from '../lib/labels'
import { useLookupList } from '../lib/lookups'
import { useBankAccountOptions } from '../lib/bankAccounts'
import type { Client, BillingMode } from '../types/api'

interface FormValues {
  name: string
  clientCode: string
  clientTypeId: number
  billingMode: BillingMode
  contactPerson: string
  phone: string
  address: string
  bankAccountId: string   // '' = use the default account (select values are strings)
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

  // Only active accounts can be picked, but keep an existing (now inactive) assignment listed so
  // editing another field doesn't silently reset it.
  const bankAccounts = useBankAccountOptions()
  const defaultAccount = (bankAccounts.data ?? []).find((a) => a.defaultAccount)
  const bankOptions = (bankAccounts.data ?? []).filter(
    (a) => a.active || a.id === client?.bankAccountId,
  )

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
      bankAccountId: client?.bankAccountId != null ? String(client.bankAccountId) : '',
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
        bankAccountId: v.bankAccountId ? Number(v.bankAccountId) : null,
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

        <div>
          <label className={label}>Rekening Transfer</label>
          <select className={field} {...register('bankAccountId')}>
            <option value="">
              {defaultAccount ? `— Gunakan default (${defaultAccount.label}) —` : '— Gunakan rekening default —'}
            </option>
            {bankOptions.map((a) => (
              <option key={a.id} value={String(a.id)}>
                {a.active ? a.label : `${a.label} (nonaktif)`}
              </option>
            ))}
          </select>
          <p className="mt-1 text-xs text-gray-400">
            Rekening tujuan transfer pada tagihan bulanan klien ini. Tagihan yang sudah diterbitkan
            tidak berubah.
          </p>
        </div>

        <ModalFormActions onClose={onClose} error={mutation.error} isPending={mutation.isPending} />
      </form>
    </Modal>
  )
}
