// ── Shared API response types ─────────────────────────────────────────────────
// These mirror the backend DTOs. Keep in sync with the Java record definitions.

/** Paged list response — mirrors the backend shared.Page record. `page` is 0-based. */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** Reference data (item units, item categories, client types) — managed via Master Data. */
export interface Lookup {
  id:          number
  code:        string
  displayName: string
  sortOrder:   number
  active:      boolean
}

export interface Item {
  id:         number
  name:       string
  unitId:     number   // → item-units lookup
  categoryId: number   // → item-categories lookup
  active:     boolean
}

// BillingMode stays a fixed enum (it drives billing logic).
export type BillingMode = 'COMBINED' | 'PER_DEPARTMENT'

export interface Client {
  id:            number
  name:          string
  clientCode:    string
  clientTypeId:  number   // → client-types lookup
  billingMode:   BillingMode
  contactPerson: string | null
  phone:         string | null
  address:       string | null
  orderToken:    string
  active:        boolean
  createdAt:     string   // ISO-8601 instant string
}

export interface Department {
  id:       number
  clientId: number
  name:     string
  active:   boolean
}

export interface PriceListEntry {
  itemId:       number
  pricePerUnit: number
  effectiveDate: string   // ISO date string
}
