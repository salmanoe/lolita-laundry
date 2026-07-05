import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { indexById, useLookupList } from '../lib/lookups'
import OrderItemPicker, { toLineItems, type QuantityMap } from './OrderItemPicker'
import type { Department, Item, Order, OrderFormItem, PriceListEntry } from '../types/api'

const rupiah = (n: number) =>
  new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)

interface Props {
  open: boolean
  onClose: () => void
  order: Order
  clientId: number
}

/**
 * SUPER_ADMIN item correction on a LOCKED order (DONE/DELIVERED) — the affordance for fixing a
 * wrong item picked by DAILY_STAFF after the normal edit window closed. Items only (date/Treatment
 * are separate RECEIVED/PROCESSING corrections); re-priced server-side at the frozen order date via
 * `PUT /api/orders/{id}/items`. Rejected by the backend if the order is on an issued billing.
 */
export default function CorrectItemsModal({ open, onClose, order, clientId }: Readonly<Props>) {
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const token = async () => getAccessTokenSilently()

  const itemsQ = useQuery({
    queryKey: ['items', 'options'],
    enabled: open,
    queryFn: async () => apiFetch<Item[]>('/api/items/options', { token: await token() }),
  })
  const pricesQ = useQuery({
    queryKey: ['prices', clientId],
    enabled: open,
    queryFn: async () => apiFetch<PriceListEntry[]>(`/api/clients/${clientId}/prices`, { token: await token() }),
  })
  const deptsQ = useQuery({
    queryKey: ['departments', clientId],
    enabled: open,
    queryFn: async () => apiFetch<Department[]>(`/api/clients/${clientId}/departments`, { token: await token() }),
  })
  const unitsById = indexById(useLookupList('item-units').data)
  const deptNameById = useMemo(
    () => new Map((deptsQ.data ?? []).map((d) => [d.id, d.name])),
    [deptsQ.data],
  )

  const [quantities, setQuantities] = useState<QuantityMap>(() =>
    Object.fromEntries(order.lineItems.map((li) => [li.itemId, String(li.quantity)])),
  )

  // The order's multiplier is fixed here (Treatment is corrected separately) — drive the price
  // preview off it so subtotals match what the server will compute.
  const multiplier = order.pricingMultiplier

  // Orderable items = those with a current price, plus any already on this order (so existing lines
  // stay visible/editable even if their price entry has since changed).
  const pickerItems = useMemo<OrderFormItem[]>(() => {
    const priceById = new Map((pricesQ.data ?? []).map((p) => [p.itemId, p.pricePerUnit]))
    const priceDeptById = new Map((pricesQ.data ?? []).map((p) => [p.itemId, p.departmentId]))
    const onOrder = new Map(order.lineItems.map((li) => [li.itemId, li.priceAtOrder]))
    const onOrderDept = new Map(order.lineItems.map((li) => [li.itemId, li.departmentId]))
    return (itemsQ.data ?? [])
      .filter((i) => priceById.has(i.id) || onOrder.has(i.id))
      .map((i) => ({
        itemId: i.id,
        name: i.name,
        unitId: i.unitId,
        unitName: unitsById.get(i.unitId)?.displayName ?? null,
        departmentId: priceDeptById.get(i.id) ?? onOrderDept.get(i.id) ?? null,
        price: priceById.get(i.id) ?? onOrder.get(i.id) ?? 0,
      }))
  }, [itemsQ.data, pricesQ.data, order.lineItems, unitsById])

  const lines = toLineItems(quantities)
  const total = useMemo(() => {
    const priceById = new Map(pickerItems.map((i) => [i.itemId, i.price]))
    return lines.reduce((s, l) => s + (priceById.get(l.itemId) ?? 0) * multiplier * l.quantity, 0)
  }, [lines, pickerItems, multiplier])

  // Close on Escape (keyboard a11y) while the modal is open.
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    globalThis.addEventListener('keydown', onKey)
    return () => globalThis.removeEventListener('keydown', onKey)
  }, [open, onClose])

  const save = useMutation({
    mutationFn: async () =>
      apiFetch<Order>(`/api/orders/${order.id}/items`, {
        method: 'PUT',
        token: await token(),
        body: JSON.stringify(lines),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['order', order.id] })
      qc.invalidateQueries({ queryKey: ['order-history', order.id] })
      qc.invalidateQueries({ queryKey: ['orders'] })
      qc.invalidateQueries({ queryKey: ['billings'] })   // re-totals the monthly billing
      onClose()
    },
  })

  if (!open) return null

  const loading = itemsQ.isLoading || pricesQ.isLoading

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto p-4">
      {/* Backdrop as a native button → click / keyboard dismiss without a div click-handler. */}
      <button type="button" aria-label="Tutup" onClick={onClose} className="fixed inset-0 cursor-default bg-black/40" />
      <div className="relative z-10 my-8 w-full max-w-2xl rounded-xl bg-white shadow-xl">
        <div className="flex items-center justify-between border-b px-6 py-4">
          <h2 className="text-lg font-semibold text-gray-800">Koreksi Item {order.orderNumber}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600" aria-label="Tutup">✕</button>
        </div>

        <div className="max-h-[70vh] space-y-5 overflow-y-auto px-6 py-5">
          <p className="rounded-lg bg-amber-50 px-4 py-3 text-xs text-amber-700">
            Koreksi item untuk order yang sudah {orderLabel(order.status)}. Digunakan untuk memperbaiki
            item yang salah dipilih. Harga dihitung ulang pada tanggal order. Tidak dapat dikoreksi jika
            order sudah masuk tagihan yang telah diterbitkan.
          </p>
          <div>
            <p className="mb-2 text-sm font-semibold text-gray-700">Item</p>
            {loading ? (
              <p className="text-sm text-gray-400">Memuat item…</p>
            ) : (
              <OrderItemPicker
                items={pickerItems}
                departmentName={(did) => deptNameById.get(did) ?? '—'}
                unitName={(uid) => unitsById.get(uid)?.displayName ?? '—'}
                multiplier={multiplier}
                showPrices
                quantities={quantities}
                onChange={setQuantities}
              />
            )}
          </div>

          {save.error && (
            <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
              {save.error instanceof ApiError ? save.error.detail : 'Gagal menyimpan koreksi item.'}
            </p>
          )}
        </div>

        <div className="flex items-center justify-between gap-4 border-t px-6 py-4">
          <div>
            <p className="text-xs text-gray-400">{lines.length} item</p>
            <p className="text-base font-bold text-gray-800">{rupiah(total)}</p>
          </div>
          <div className="flex gap-2">
            <button onClick={onClose} className="rounded-lg px-4 py-2 text-sm text-gray-600 hover:bg-gray-100">
              Batal
            </button>
            <button
              onClick={() => save.mutate()}
              disabled={lines.length === 0 || save.isPending}
              className="rounded-lg bg-brand-600 px-5 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {save.isPending ? 'Menyimpan…' : 'Simpan Koreksi'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

function orderLabel(status: Order['status']): string {
  return status === 'DELIVERED' ? 'dikirim' : 'selesai'
}
