import { useMemo, useState } from 'react'
import type { OrderFormItem } from '../types/api'

const rupiah = (n: number) =>
  new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)

/** Quantities keyed by itemId, kept as raw input strings (allows decimals for m² items). */
export type QuantityMap = Record<number, string>

interface Props {
  items: OrderFormItem[]
  /** Lookup department id → display name. */
  departmentName: (id: number) => string
  unitName: (id: number) => string
  /** Treatment doubles displayed unit prices (×2) when applicable. Only used when showPrices. */
  multiplier?: number
  /** Show unit price + line subtotal. Off for the public form (hotel staff never see prices). */
  showPrices?: boolean
  quantities: QuantityMap
  onChange: (next: QuantityMap) => void
}

const NO_DEPT = -1 // sentinel group key for items without a department (COMBINED clients)

/**
 * Department-grouped item picker for building an order (V9). Items with no department
 * (COMBINED clients) render as a single flat list with no header. Parent owns the quantity
 * map; this renders rows + per-line subtotals. A search box appears once there are many items.
 */
export default function OrderItemPicker({
  items,
  departmentName,
  unitName,
  multiplier = 1,
  showPrices = true,
  quantities,
  onChange,
}: Props) {
  const [search, setSearch] = useState('')

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    return q ? items.filter((i) => i.name.toLowerCase().includes(q)) : items
  }, [items, search])

  // Group filtered items by department, preserving the server's item order within each group.
  const groups = useMemo(() => {
    const byDept = new Map<number, OrderFormItem[]>()
    for (const item of filtered) {
      const key = item.departmentId ?? NO_DEPT
      const list = byDept.get(key) ?? []
      list.push(item)
      byDept.set(key, list)
    }
    return [...byDept.entries()].sort((a, b) => departmentName(a[0]).localeCompare(departmentName(b[0])))
  }, [filtered, departmentName])

  // Headers only matter when items actually belong to departments (PER_DEPARTMENT clients).
  const showHeaders = groups.some(([key]) => key !== NO_DEPT)

  function setQty(itemId: number, value: string) {
    const next = { ...quantities }
    if (value === '' || Number(value) === 0) delete next[itemId]
    else next[itemId] = value
    onChange(next)
  }

  return (
    <div className="space-y-4">
      {items.length > 8 && (
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Cari item..."
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
        />
      )}

      {groups.map(([deptId, catItems]) => (
        <div key={deptId} className="overflow-hidden rounded-lg border bg-white">
          {showHeaders && (
            <div className="bg-gray-50 px-4 py-2 text-xs font-semibold uppercase tracking-wide text-gray-500">
              {deptId === NO_DEPT ? 'Lainnya' : departmentName(deptId)}
            </div>
          )}
          <div className="divide-y divide-gray-100">
            {catItems.map((item) => {
              const raw = quantities[item.itemId] ?? ''
              const qty = Number(raw) || 0
              const unitPrice = (item.price ?? 0) * multiplier
              const subtotal = unitPrice * qty
              return (
                <div key={item.itemId} className="flex items-center gap-3 px-4 py-2.5">
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-gray-800">{item.name}</p>
                    <p className="text-xs text-gray-400">
                      {showPrices ? `${rupiah(unitPrice)} / ${unitName(item.unitId)}` : unitName(item.unitId)}
                    </p>
                  </div>
                  {showPrices && qty > 0 && (
                    <span className="hidden text-sm font-medium text-gray-600 sm:block">{rupiah(subtotal)}</span>
                  )}
                  <input
                    type="number"
                    inputMode="decimal"
                    min="0"
                    step="any"
                    value={raw}
                    onChange={(e) => setQty(item.itemId, e.target.value)}
                    placeholder="0"
                    className="w-20 rounded-lg border border-gray-300 px-2 py-1.5 text-right text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
                  />
                </div>
              )
            })}
          </div>
        </div>
      ))}

      {groups.length === 0 && (
        <p className="rounded-lg border bg-white px-4 py-6 text-center text-sm text-gray-400">
          Tidak ada item yang cocok.
        </p>
      )}
    </div>
  )
}

/** Builds the line-items payload from a quantity map (drops zero/empty rows). */
export function toLineItems(quantities: QuantityMap): { itemId: number; quantity: number }[] {
  return Object.entries(quantities)
    .map(([itemId, raw]) => ({ itemId: Number(itemId), quantity: Number(raw) }))
    .filter((l) => l.quantity > 0)
}