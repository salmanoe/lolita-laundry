import { useForm } from 'react-hook-form'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import Modal from './Modal'
import { useLookupList, type LookupKind } from '../lib/lookups'
import type { Lookup } from '../types/api'

interface FormValues {
  code: string
  displayName: string
  active: boolean
}

interface Props {
  open: boolean
  onClose: () => void
  kind: LookupKind
  title: string
  entry?: Lookup // undefined → create mode
}

const field =
  'w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'
const label = 'block text-xs font-medium text-gray-600 mb-1'

export default function LookupFormModal({ open, onClose, kind, title, entry }: Props) {
  const editing = !!entry
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const { data: list } = useLookupList(kind)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    values: {
      code: entry?.code ?? '',
      displayName: entry?.displayName ?? '',
      active: entry?.active ?? true,
    },
  })

  const mutation = useMutation({
    mutationFn: async (v: FormValues) => {
      const token = await getAccessTokenSilently()
      if (editing) {
        // Preserve existing order on edit — reordering is done with the ↑/↓ buttons in the list.
        return apiFetch(`/api/${kind}/${entry!.id}`, {
          method: 'PUT',
          token,
          body: JSON.stringify({ displayName: v.displayName, sortOrder: entry!.sortOrder, active: v.active }),
        })
      }
      // Append to the end automatically — no manual order entry.
      const nextOrder = Math.max(0, ...(list ?? []).map((l) => l.sortOrder)) + 1
      return apiFetch(`/api/${kind}`, {
        method: 'POST',
        token,
        body: JSON.stringify({ code: v.code, displayName: v.displayName, sortOrder: nextOrder }),
      })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [kind] })
      onClose()
    },
  })

  return (
    <Modal open={open} title={editing ? `Ubah ${title}` : `Tambah ${title}`} onClose={onClose}>
      <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-3">
        <div>
          <label className={label}>Kode</label>
          <input
            className={field + (editing ? ' bg-gray-100 text-gray-500' : '')}
            readOnly={editing}
            placeholder="PCS"
            {...register('code', { required: !editing, pattern: /^[A-Z0-9_]+$/ })}
          />
          {editing ? (
            <p className="mt-1 text-xs text-gray-400">Kode tidak dapat diubah.</p>
          ) : (
            errors.code && <p className="mt-1 text-xs text-red-600">Huruf kapital/angka/garis bawah (mis. PCS).</p>
          )}
        </div>

        <div>
          <label className={label}>Nama Tampilan</label>
          <input className={field} placeholder="Pcs" {...register('displayName', { required: true })} />
          {errors.displayName && <p className="mt-1 text-xs text-red-600">Nama wajib diisi.</p>}
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
