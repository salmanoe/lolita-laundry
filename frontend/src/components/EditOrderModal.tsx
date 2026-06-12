import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useMe } from '../auth/useMe'
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
 * Edits an in-flight order (RECEIVED|PROCESSING): dueDate, notes and line items.
 * Items are re-priced server-side on save; this rebuilds the same item picker the public
 * form uses, seeded with the order's current quantities and the client's current prices.
 */
export default function EditOrderModal({ open, onClose, order, clientId }: Props) {
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const token = async () => getAccessTokenSilently()
  // DAILY_STAFF are price-free: they edit quantities without seeing unit prices or totals.
  const isDailyStaff = useMe().data?.role === 'DAILY_STAFF'

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

  const [dueDate, setDueDate] = useState(order.dueDate ?? '')
  const [notes, setNotes] = useState(order.notes ?? '')
  const [quantities, setQuantities] = useState<QuantityMap>(() =>
    Object.fromEntries(order.lineItems.map((li) => [li.itemId, String(li.quantity)])),
  )

  const multiplier = order.pricingMultiplier

  // Orderable items = those with a current price, plus any already on this order (so the
  // existing lines remain visible/editable even if their price entry has since changed).
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

  const save = useMutation({
    mutationFn: async () =>
      apiFetch<Order>(`/api/orders/${order.id}`, {
        method: 'PUT',
        token: await token(),
        body: JSON.stringify({
          dueDate: dueDate || null,
          notes: notes.trim() || null,
          items: lines,
        }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['order', order.id] })
      onClose()
    },
  })

  if (!open) return null

  const loading = itemsQ.isLoading || pricesQ.isLoading

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-4" onClick={onClose}>
      <div
        className="my-8 w-full max-w-2xl rounded-xl bg-white shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b px-6 py-4">
          <h2 className="text-lg font-semibold text-gray-800">Ubah Order {order.orderNumber}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600" aria-label="Tutup">✕</button>
        </div>

        <div className="max-h-[70vh] space-y-5 overflow-y-auto px-6 py-5">
          <div className="grid gap-4 md:grid-cols-2">
            <label className="block">
              <span className="mb-1 block text-xs font-medium text-gray-500">Jatuh Tempo</span>
              <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} className={inputCls} />
            </label>
            <label className="block">
              <span className="mb-1 block text-xs font-medium text-gray-500">Catatan</span>
              <input value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Opsional" className={inputCls} />
            </label>
          </div>

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
                showPrices={!isDailyStaff}
                quantities={quantities}
                onChange={setQuantities}
              />
            )}
          </div>

          {save.error && (
            <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
              {save.error instanceof ApiError ? save.error.detail : 'Gagal menyimpan order.'}
            </p>
          )}
        </div>

        <div className="flex items-center justify-between gap-4 border-t px-6 py-4">
          <div>
            <p className="text-xs text-gray-400">{lines.length} item</p>
            {!isDailyStaff && <p className="text-base font-bold text-gray-800">{rupiah(total)}</p>}
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
              {save.isPending ? 'Menyimpan…' : 'Simpan'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

const inputCls =
  'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'
