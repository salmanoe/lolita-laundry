import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useMe } from '../auth/useMe'
import type { DeliveryConfirmation, DriverDelivery } from '../types/api'

export default function DeliveriesPage() {
  const { getAccessTokenSilently } = useAuth()
  const token = async () => getAccessTokenSilently()
  const [hotel, setHotel] = useState('')

  const deliveriesQ = useQuery({
    queryKey: ['driver-deliveries'],
    queryFn: async () => apiFetch<DriverDelivery[]>('/api/deliveries', { token: await token() }),
  })

  if (deliveriesQ.isLoading) return <div className="text-sm text-gray-400">Memuat pengiriman…</div>
  if (deliveriesQ.error) return <div className="text-sm text-red-500">Gagal memuat pengiriman.</div>

  const deliveries = deliveriesQ.data ?? []
  // Hotel options come from the pool itself — drivers can't call the admin /api/clients endpoint.
  const hotels = [...new Set(deliveries.map((d) => d.clientName))].sort((a, b) => a.localeCompare(b))
  const shown = hotel ? deliveries.filter((d) => d.clientName === hotel) : deliveries

  return (
    <div className="space-y-4">
      {/* Screen title lives in the DriverLayout header — no page heading here. */}
      {deliveries.length > 0 && (
        <select
          value={hotel}
          onChange={(e) => setHotel(e.target.value)}
          aria-label="Filter hotel"
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
        >
          <option value="">Semua Hotel</option>
          {hotels.map((h) => (
            <option key={h} value={h}>{h}</option>
          ))}
        </select>
      )}

      {deliveries.length === 0 ? (
        <p className="rounded-lg border border-dashed bg-white p-8 text-center text-sm text-gray-400">
          Tidak ada order untuk dikirim saat ini.
        </p>
      ) : shown.length === 0 ? (
        <p className="rounded-lg border border-dashed bg-white p-8 text-center text-sm text-gray-400">
          Tidak ada order untuk hotel ini.
        </p>
      ) : (
        shown.map((d) => <DeliveryCard key={d.orderId} delivery={d} />)
      )}
    </div>
  )
}

function DeliveryCard({ delivery }: { delivery: DriverDelivery }) {
  const ready = delivery.status === 'DONE'
  const [open, setOpen] = useState(false)

  return (
    <div className="rounded-xl border bg-white shadow-sm">
      <div className="flex items-start justify-between gap-3 p-4">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-mono text-sm font-semibold text-gray-800">{delivery.orderNumber}</span>
            <span
              className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                ready ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
              }`}
            >
              {ready ? 'Siap dikirim' : 'Belum ditandai Selesai'}
            </span>
          </div>
          <p className="mt-1 text-sm text-gray-700">{delivery.clientName}</p>
          {delivery.departmentName && <p className="text-xs text-gray-400">{delivery.departmentName}</p>}
        </div>
        <div className="shrink-0 text-right text-xs text-gray-400">
          <p>Order: {delivery.orderDate}</p>
          {delivery.dueDate && <p>Tempo: {delivery.dueDate}</p>}
        </div>
      </div>

      {/* Items — what to deliver, quantities only, never prices */}
      <ul className="border-t px-4 py-3 text-sm">
        {delivery.lines.map((l, i) => (
          <li key={i} className="flex justify-between py-0.5 text-gray-700">
            <span>{l.itemName}</span>
            <span className="text-gray-500">
              {l.quantity} {l.unitName ?? ''}
            </span>
          </li>
        ))}
      </ul>
      {delivery.notes && <p className="px-4 pb-2 text-xs text-gray-500">Catatan: {delivery.notes}</p>}

      <div className="border-t p-4">
        {!ready && !open && (
          <p className="mb-2 text-xs text-amber-600">
            Order ini belum ditandai Selesai oleh staf — tetap bisa dikonfirmasi bila barang sudah diantar.
          </p>
        )}
        {open ? (
          <ConfirmForm delivery={delivery} onCancel={() => setOpen(false)} />
        ) : (
          <button
            onClick={() => setOpen(true)}
            className="w-full rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700"
          >
            Konfirmasi Terkirim
          </button>
        )}
      </div>
    </div>
  )
}

function ConfirmForm({ delivery, onCancel }: { delivery: DriverDelivery; onCancel: () => void }) {
  const { getAccessTokenSilently } = useAuth()
  const meQ = useMe()
  const qc = useQueryClient()
  const token = async () => getAccessTokenSilently()

  const [recipient, setRecipient] = useState('')
  const [deliverer, setDeliverer] = useState(meQ.data?.fullName ?? '')
  const [notes, setNotes] = useState('')
  const [photo, setPhoto] = useState<File | null>(null)

  const confirm = useMutation({
    mutationFn: async () => {
      const fd = new FormData()
      fd.append('recipientName', recipient.trim())
      fd.append('delivererName', deliverer.trim())
      if (notes.trim()) fd.append('notes', notes.trim())
      fd.append('photo', photo!)
      return apiFetch<DeliveryConfirmation>(`/api/deliveries/${delivery.orderId}/confirm`, {
        method: 'POST',
        token: await token(),
        body: fd,
      })
    },
    // On success the order becomes DELIVERED and drops off the list.
    onSuccess: () => qc.invalidateQueries({ queryKey: ['driver-deliveries'] }),
  })

  const canSubmit = recipient.trim() && deliverer.trim() && photo && !confirm.isPending

  return (
    <div className="space-y-3">
      <Labeled label="Nama Penerima" required>
        <input value={recipient} onChange={(e) => setRecipient(e.target.value)} className={inputCls} />
      </Labeled>
      <Labeled label="Nama Pengantar" required>
        <input value={deliverer} onChange={(e) => setDeliverer(e.target.value)} className={inputCls} />
      </Labeled>
      <Labeled label="Catatan">
        <input value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Opsional" className={inputCls} />
      </Labeled>
      <Labeled label="Foto Bukti" required>
        <input
          type="file"
          accept="image/*"
          capture="environment"
          onChange={(e) => setPhoto(e.target.files?.[0] ?? null)}
          className="block w-full text-sm text-gray-600 file:mr-3 file:rounded-lg file:border-0 file:bg-brand-50 file:px-4 file:py-2 file:text-sm file:font-medium file:text-brand-700 hover:file:bg-brand-100"
        />
      </Labeled>
      {confirm.error && (
        <p className="text-sm text-red-500">
          {confirm.error instanceof ApiError ? confirm.error.detail : 'Gagal menyimpan konfirmasi.'}
        </p>
      )}
      <div className="flex gap-2">
        <button
          onClick={() => confirm.mutate()}
          disabled={!canSubmit}
          className="flex-1 rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {confirm.isPending ? 'Menyimpan…' : 'Simpan Konfirmasi'}
        </button>
        <button
          onClick={onCancel}
          className="rounded-lg border border-gray-300 px-4 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-50"
        >
          Batal
        </button>
      </div>
    </div>
  )
}

const inputCls =
  'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'

function Labeled({ label, required, children }: { label: string; required?: boolean; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs font-medium text-gray-500">
        {label} {required && <span className="text-red-500">*</span>}
      </span>
      {children}
    </label>
  )
}