import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import EditOrderModal from '../components/EditOrderModal'
import { nextAdvanceStatus, orderStatusBadge, orderStatusLabel } from '../lib/labels'
import type {
  Client,
  DeliveryConfirmation,
  Department,
  Item,
  Order,
  StatusHistoryEntry,
} from '../types/api'

const rupiah = (n: number) =>
  new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)

export default function OrderDetailPage() {
  const { id } = useParams()
  const orderId = Number(id)
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const token = async () => getAccessTokenSilently()

  const [editOpen, setEditOpen] = useState(false)

  const orderQ = useQuery({
    queryKey: ['order', orderId],
    queryFn: async () => apiFetch<Order>(`/api/orders/${orderId}`, { token: await token() }),
  })
  const historyQ = useQuery({
    queryKey: ['order-history', orderId],
    queryFn: async () => apiFetch<StatusHistoryEntry[]>(`/api/orders/${orderId}/history`, { token: await token() }),
  })
  const itemsQ = useQuery({
    queryKey: ['items', 'options'],
    queryFn: async () => apiFetch<Item[]>('/api/items/options', { token: await token() }),
  })

  const order = orderQ.data
  const clientQ = useQuery({
    queryKey: ['client', order?.clientId],
    enabled: !!order,
    queryFn: async () => apiFetch<Client>(`/api/clients/${order!.clientId}`, { token: await token() }),
  })
  const deptsQ = useQuery({
    queryKey: ['departments', order?.clientId],
    enabled: !!order?.departmentId,
    queryFn: async () =>
      apiFetch<Department[]>(`/api/clients/${order!.clientId}/departments`, { token: await token() }),
  })

  const advance = useMutation({
    mutationFn: async (status: string) =>
      apiFetch<Order>(`/api/orders/${orderId}/status`, {
        method: 'PATCH',
        token: await token(),
        body: JSON.stringify({ status }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['order', orderId] })
      qc.invalidateQueries({ queryKey: ['order-history', orderId] })
    },
  })

  if (orderQ.isLoading) return <div className="text-sm text-gray-400">Memuat order...</div>
  if (orderQ.error || !order) return <div className="text-sm text-red-500">Gagal memuat order.</div>

  const itemsById = new Map((itemsQ.data ?? []).map((i) => [i.id, i]))
  const deptName = deptsQ.data?.find((d) => d.id === order.departmentId)?.name
  const next = nextAdvanceStatus[order.status]
  const editable = order.status === 'RECEIVED' || order.status === 'PROCESSING'

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <Link to="/orders" className="text-sm text-gray-500 hover:text-gray-700">← Order</Link>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="font-mono text-xl font-semibold text-gray-800">{order.orderNumber}</h1>
            <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${orderStatusBadge[order.status]}`}>
              {orderStatusLabel[order.status]}
            </span>
            {order.pricingMultiplier > 1 && (
              <span className="rounded-full bg-rose-100 px-2 py-0.5 text-xs font-medium text-rose-700">Treatment</span>
            )}
          </div>
          <div className="flex gap-2">
            {editable && (
              <button
                onClick={() => setEditOpen(true)}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                Ubah Order
              </button>
            )}
            {next && (
              <button
                onClick={() => advance.mutate(next)}
                disabled={advance.isPending}
                className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-50"
              >
                {advance.isPending ? 'Memproses…' : `Tandai ${orderStatusLabel[next]}`}
              </button>
            )}
          </div>
        </div>
        {advance.error && (
          <p className="mt-2 text-sm text-red-500">
            {advance.error instanceof ApiError ? advance.error.detail : 'Gagal mengubah status.'}
          </p>
        )}
      </div>

      {/* Info */}
      <dl className="grid grid-cols-2 gap-x-6 gap-y-4 rounded-lg border bg-white p-6 text-sm shadow-sm md:grid-cols-4">
        <Info label="Klien" value={clientQ.data?.name ?? '—'} />
        {order.departmentId && <Info label="Departemen" value={deptName ?? '—'} />}
        <Info label="Tanggal Order" value={order.orderDate} />
        <Info label="Jatuh Tempo" value={order.dueDate ?? '—'} />
        <Info label="Staff Pengirim" value={order.submittedByName ?? '—'} />
        <Info label="Total" value={<span className="font-semibold">{rupiah(order.total)}</span>} />
        {order.notes && <Info label="Catatan" value={order.notes} />}
      </dl>

      {/* Line items */}
      <section>
        <h2 className="mb-3 text-base font-semibold text-gray-800">Item</h2>
        <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['Item', 'Qty', 'Harga Satuan', 'Subtotal'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {order.lineItems.map((li) => (
                <tr key={li.id}>
                  <td className="px-4 py-3 text-gray-800">{itemsById.get(li.itemId)?.name ?? `#${li.itemId}`}</td>
                  <td className="px-4 py-3 text-gray-500">{li.quantity}</td>
                  <td className="px-4 py-3 text-gray-500">{rupiah(li.priceAtOrder * order.pricingMultiplier)}</td>
                  <td className="px-4 py-3 font-medium text-gray-700">{rupiah(li.subtotal)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="border-t bg-gray-50">
                <td colSpan={3} className="px-4 py-3 text-right font-medium text-gray-600">Total</td>
                <td className="px-4 py-3 font-bold text-gray-800">{rupiah(order.total)}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>

      {/* Delivery */}
      <DeliverySection orderId={orderId} order={order} />

      {/* History */}
      <section>
        <h2 className="mb-3 text-base font-semibold text-gray-800">Riwayat Status</h2>
        <ol className="space-y-3 rounded-lg border bg-white p-5 text-sm shadow-sm">
          {(historyQ.data ?? []).map((h) => (
            <li key={h.id} className="flex items-start gap-3">
              <span className={`mt-0.5 inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${orderStatusBadge[h.toStatus]}`}>
                {orderStatusLabel[h.toStatus]}
              </span>
              <div className="min-w-0">
                <p className="text-gray-600">{h.notes ?? '—'}</p>
                <p className="text-xs text-gray-400">{new Date(h.changedAt).toLocaleString('id-ID')}</p>
              </div>
            </li>
          ))}
          {historyQ.data?.length === 0 && <li className="text-gray-400">Belum ada riwayat.</li>}
        </ol>
      </section>

      <EditOrderModal
        open={editOpen}
        order={order}
        clientId={order.clientId}
        onClose={() => setEditOpen(false)}
      />
    </div>
  )
}

/** Delivery: upload form when the order is DONE, read-only confirmation once DELIVERED. */
function DeliverySection({ orderId, order }: { orderId: number; order: Order }) {
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const token = async () => getAccessTokenSilently()

  const [recipient, setRecipient] = useState('')
  const [deliverer, setDeliverer] = useState('')
  const [notes, setNotes] = useState('')
  const [photo, setPhoto] = useState<File | null>(null)

  const deliveryQ = useQuery({
    queryKey: ['delivery', orderId],
    enabled: order.status === 'DELIVERED',
    queryFn: async () => apiFetch<DeliveryConfirmation>(`/api/orders/${orderId}/delivery`, { token: await token() }),
  })
  const photoUrlQ = useQuery({
    queryKey: ['delivery-photo', orderId],
    enabled: order.status === 'DELIVERED' && !!deliveryQ.data,
    queryFn: async () =>
      apiFetch<{ url: string }>(`/api/orders/${orderId}/delivery/photo-url`, { token: await token() }),
  })

  const deliver = useMutation({
    mutationFn: async () => {
      const fd = new FormData()
      fd.append('recipientName', recipient.trim())
      fd.append('delivererName', deliverer.trim())
      if (notes.trim()) fd.append('notes', notes.trim())
      fd.append('photo', photo!)
      return apiFetch<DeliveryConfirmation>(`/api/orders/${orderId}/delivery`, {
        method: 'POST',
        token: await token(),
        body: fd,
      })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['order', orderId] })
      qc.invalidateQueries({ queryKey: ['order-history', orderId] })
      qc.invalidateQueries({ queryKey: ['delivery', orderId] })
    },
  })

  if (order.status === 'RECEIVED' || order.status === 'PROCESSING') return null

  if (order.status === 'DELIVERED') {
    const d = deliveryQ.data
    return (
      <section>
        <h2 className="mb-3 text-base font-semibold text-gray-800">Konfirmasi Pengiriman</h2>
        <div className="grid gap-6 rounded-lg border bg-white p-6 text-sm shadow-sm md:grid-cols-[1fr_auto]">
          <dl className="grid grid-cols-2 gap-x-6 gap-y-4">
            <Info label="Nama Penerima" value={d?.recipientName ?? '—'} />
            <Info label="Nama Pengantar" value={d?.delivererName ?? '—'} />
            <Info label="Waktu" value={d ? new Date(d.deliveredAt).toLocaleString('id-ID') : '—'} />
            {d?.notes && <Info label="Catatan" value={d.notes} />}
          </dl>
          {photoUrlQ.data?.url && (
            <a href={photoUrlQ.data.url} target="_blank" rel="noreferrer" className="shrink-0">
              <img
                src={photoUrlQ.data.url}
                alt="Foto pengiriman"
                className="h-40 w-40 rounded-lg border object-cover"
              />
            </a>
          )}
        </div>
      </section>
    )
  }

  // status === DONE → upload form
  const canSubmit = recipient.trim() && deliverer.trim() && photo && !deliver.isPending
  return (
    <section>
      <h2 className="mb-3 text-base font-semibold text-gray-800">Konfirmasi Pengiriman</h2>
      <div className="space-y-4 rounded-lg border bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-2">
          <Labeled label="Nama Penerima" required>
            <input value={recipient} onChange={(e) => setRecipient(e.target.value)} className={inputCls} />
          </Labeled>
          <Labeled label="Nama Pengantar" required>
            <input value={deliverer} onChange={(e) => setDeliverer(e.target.value)} className={inputCls} />
          </Labeled>
        </div>
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
        {deliver.error && (
          <p className="text-sm text-red-500">
            {deliver.error instanceof ApiError ? deliver.error.detail : 'Gagal menyimpan konfirmasi.'}
          </p>
        )}
        <button
          onClick={() => deliver.mutate()}
          disabled={!canSubmit}
          className="rounded-lg bg-brand-600 px-5 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {deliver.isPending ? 'Menyimpan…' : 'Konfirmasi Terkirim'}
        </button>
      </div>
    </section>
  )
}

const inputCls =
  'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'

function Info({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs font-medium text-gray-400">{label}</dt>
      <dd className="mt-0.5 text-gray-800">{value}</dd>
    </div>
  )
}

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
