// Static label for the one enum that stays fixed in code (BillingMode drives billing logic).
// Item units, item categories and client types are dynamic reference data — resolve their
// labels from the lookup lists (see lib/lookups.ts), not from here.
import type { BillingMode, OrderStatus } from '../types/api'

export const billingModeLabel: Record<BillingMode, string> = {
  COMBINED: 'Gabungan',
  PER_DEPARTMENT: 'Per Departemen',
}

// Order status — Indonesian label + badge colour. Flow: RECEIVED → PROCESSING → DONE → DELIVERED.
export const orderStatusLabel: Record<OrderStatus, string> = {
  RECEIVED: 'Diterima',
  PROCESSING: 'Diproses',
  DONE: 'Selesai',
  DELIVERED: 'Terkirim',
}

export const orderStatusBadge: Record<OrderStatus, string> = {
  RECEIVED: 'bg-amber-100 text-amber-700',
  PROCESSING: 'bg-blue-100 text-blue-700',
  DONE: 'bg-violet-100 text-violet-700',
  DELIVERED: 'bg-green-100 text-green-700',
}

/** The next status an order can advance to via PATCH /status (never DELIVERED — use delivery endpoint). */
export const nextAdvanceStatus: Partial<Record<OrderStatus, OrderStatus>> = {
  RECEIVED: 'PROCESSING',
  PROCESSING: 'DONE',
}