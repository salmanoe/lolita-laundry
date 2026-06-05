// ── Shared API response types ─────────────────────────────────────────────────
// These mirror the backend DTOs. Keep in sync with the Java record definitions.

/** Lolita user roles. Hotel staff are not users (they use tokenized public links). */
export type Role = 'OWNER' | 'STAFF' | 'DRIVER'

/** Current authenticated user — mirrors MeResponse. Drives role-aware routing. */
export interface Me {
  id:       number
  fullName: string
  role:     Role
}

/** A driver option for the staff assignment picker — mirrors DriverResponse. */
export interface Driver {
  id:       number
  fullName: string
}

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

// ── Orders (Phase 2) ──────────────────────────────────────────────────────────

/** One-way lifecycle: RECEIVED → PROCESSING → DONE → DELIVERED. */
export type OrderStatus = 'RECEIVED' | 'PROCESSING' | 'DONE' | 'DELIVERED'

/** Public order-form payload — mirrors OrderFormResponse. */
export interface OrderForm {
  clientId:           number
  clientName:         string
  clientCode:         string
  perDepartment:      boolean
  treatmentAvailable: boolean
  departments:        { id: number; name: string }[]
  items:              OrderFormItem[]
}

export interface OrderFormItem {
  itemId:       number
  name:         string
  unitId:       number
  unitName:     string | null
  categoryId:   number
  categoryName: string | null
  // Public order-form items omit price (never exposed to hotel staff). The authenticated
  // staff edit screen builds OrderFormItem locally and sets this for its price preview.
  price?:       number
}

/** One line being submitted (price resolved server-side). */
export interface OrderLineRequest {
  itemId:   number
  quantity: number
}

export interface OrderLineItem {
  id:           number
  itemId:       number
  quantity:     number
  priceAtOrder: number
  subtotal:     number
}

/** Full order detail — mirrors OrderResponse. */
export interface Order {
  id:                number
  orderNumber:       string
  clientId:          number
  departmentId:      number | null
  orderDate:         string   // ISO date
  dueDate:           string | null
  status:            OrderStatus
  pricingMultiplier: number
  submittedByName:   string | null
  notes:             string | null
  createdByUserId:   number | null
  assignedDriverId:  number | null
  createdAt:         string   // ISO instant
  total:             number
  lineItems:         OrderLineItem[]
}

/** Lightweight list row — mirrors OrderSummaryResponse. */
export interface OrderSummary {
  id:                number
  orderNumber:       string
  clientId:          number
  departmentId:      number | null
  orderDate:         string
  dueDate:           string | null
  status:            OrderStatus
  pricingMultiplier: number
  submittedByName:   string | null
  assignedDriverId:  number | null
  total:             number
  createdAt:         string
}

export interface StatusHistoryEntry {
  id:              number
  orderId:         number
  fromStatus:      OrderStatus | null
  toStatus:        OrderStatus
  changedByUserId: number | null
  changedAt:       string
  notes:           string | null
}

export interface DeliveryConfirmation {
  id:            number
  orderId:       number
  deliveredAt:   string
  recipientName: string
  delivererName: string
  photoUrl:      string | null
  notes:         string | null
}

// ── Driver delivery (Phase 2 extension) ────────────────────────────────────────

/** One line on a driver's delivery — price-free by design (no unit price / subtotal). */
export interface DriverDeliveryLine {
  itemName: string
  unitName: string | null
  quantity: number
}

/** An assigned order as a driver sees it — mirrors DriverDeliveryResponse. No prices. */
export interface DriverDelivery {
  orderId:        number
  orderNumber:    string
  clientName:     string
  departmentName: string | null
  orderDate:      string
  dueDate:        string | null
  status:         OrderStatus
  notes:          string | null
  lines:          DriverDeliveryLine[]
}
