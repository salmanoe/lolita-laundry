// Static label for the one enum that stays fixed in code (BillingMode drives billing logic).
// Item units, item categories and client types are dynamic reference data — resolve their
// labels from the lookup lists (see lib/lookups.ts), not from here.
import type { BillingMode, BillingStatus, OrderStatus, Role } from '../types/api'

// User roles — Indonesian label + badge color. SUPER_ADMIN runs system config, OWNER views
// analytics, STAFF runs the admin app day-to-day, DRIVER the delivery app.
export const roleLabel: Record<Role, string> = {
  SUPER_ADMIN: 'Admin Super',
  OWNER: 'Pemilik',
  STAFF: 'Staf',
  DRIVER: 'Pengantar',
}

export const roleBadge: Record<Role, string> = {
  SUPER_ADMIN: 'bg-rose-100 text-rose-700',
  OWNER: 'bg-violet-100 text-violet-700',
  STAFF: 'bg-blue-100 text-blue-700',
  DRIVER: 'bg-amber-100 text-amber-700',
}

export const billingModeLabel: Record<BillingMode, string> = {
  COMBINED: 'Gabungan',
  PER_DEPARTMENT: 'Per Departemen',
}

// Order status — Indonesian label + badge color. Flow: RECEIVED → PROCESSING → DONE → DELIVERED.
export const orderStatusLabel: Record<OrderStatus, string> = {
  RECEIVED: 'Diterima',
  PROCESSING: 'Diproses',
  DONE: 'Selesai',
  DELIVERED: 'Terkirim',
  CANCELLED: 'Dibatalkan',
}

export const orderStatusBadge: Record<OrderStatus, string> = {
  RECEIVED: 'bg-amber-100 text-amber-700',
  PROCESSING: 'bg-blue-100 text-blue-700',
  DONE: 'bg-violet-100 text-violet-700',
  DELIVERED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-gray-200 text-gray-500',
}

/** The next status an order can advance to via PATCH /status (never DELIVERED — use delivery endpoint). */
export const nextAdvanceStatus: Partial<Record<OrderStatus, OrderStatus>> = {
  RECEIVED: 'PROCESSING',
  PROCESSING: 'DONE',
}

// Billing status — Indonesian label + badge color. Flow: DRAFT → ISSUED → PAID.
export const billingStatusLabel: Record<BillingStatus, string> = {
  DRAFT: 'Draf',
  ISSUED: 'Terbit',
  PAID: 'Lunas',
}

export const billingStatusBadge: Record<BillingStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-600',
  ISSUED: 'bg-blue-100 text-blue-700',
  PAID: 'bg-green-100 text-green-700',
}

/** The next status a billing can advance to via PATCH /status (one-way, single-step). */
export const nextBillingStatus: Partial<Record<BillingStatus, BillingStatus>> = {
  DRAFT: 'ISSUED',
  ISSUED: 'PAID',
}

/** Indonesian month names, 1-indexed (monthName[1] === 'Januari'). */
export const monthName = [
  '', 'Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni',
  'Juli', 'Agustus', 'September', 'Oktober', 'November', 'Desember',
]

/**
 * Renders a "YYYY-MM" key as an Indonesian month label. `short` → "Mei 26" (3-letter month +
 * 2-digit year, for chart axes); otherwise "Mei 2026".
 */
export function monthLabelFromYm(ym: string, short = false): string {
  const [y, m] = ym.split('-').map(Number)
  const name = monthName[m] ?? ym
  return short ? `${name.slice(0, 3)} ${String(y).slice(2)}` : `${name} ${y}`
}