// ── Shared API response types ─────────────────────────────────────────────────
// These mirror the backend DTOs. Keep in sync with the Java record definitions.

/**
 * Lolita user roles (3-role model).
 * - SUPER_ADMIN — top-level administrator (all dashboards + system config + adjustments).
 * - FINANCE_STAFF — finance/back-office: clients (read), orders, billing, reports, operational dashboard.
 * - DAILY_STAFF — in-house operator: enters orders, sees the priced order list, confirms deliveries.
 */
export type Role = 'SUPER_ADMIN' | 'FINANCE_STAFF' | 'DAILY_STAFF'

/** Current authenticated user — mirrors MeResponse. Drives role-aware routing. */
export interface Me {
  id:       number
  fullName: string
  role:     Role
}

/** A Lolita user row in the owner's user-management screen — mirrors UserResponse. */
export interface User {
  id:        number
  auth0Sub:  string
  email:     string | null
  fullName:  string
  role:      Role
  active:    boolean
  createdAt: string
}

/** A self-registered identity awaiting approval — mirrors PendingUserResponse ("Permintaan Akses"). */
export interface PendingUser {
  id:          number
  auth0Sub:    string
  email:       string | null
  fullName:    string | null
  requestedAt: string   // ISO instant
}

/** Paged list response — mirrors the backend shared.Page record. `page` is 0-based. */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** Company letterhead + bank details printed on PDFs — singleton, edited in Master Data (SUPER_ADMIN only). */
export interface CompanyProfile {
  companyName:     string
  address:         string
  phone:           string
  bankBeneficiary: string
  bankName:        string
  bankAccount:     string
  bankHolder:      string
}

/** Reference data (item units, client types) — managed via Master Data. */
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

/** Lightweight client option for the order-form hotel dropdown — mirrors ClientOptionResponse. */
export interface ClientOption {
  id:         number
  clientCode: string
  name:       string
}

export interface PriceListEntry {
  itemId:        number
  pricePerUnit:  number
  effectiveDate: string        // ISO date string
  departmentId:  number | null // item→department mapping (PER_DEPARTMENT clients only)
}

// ── Orders (Phase 2) ──────────────────────────────────────────────────────────

/** Lifecycle: RECEIVED → PROCESSING → DONE → DELIVERED, with CANCELLED as a terminal off-ramp. */
export type OrderStatus = 'RECEIVED' | 'PROCESSING' | 'DONE' | 'DELIVERED' | 'CANCELLED'

/** Order-form payload for the in-house "Buat Order" screen — mirrors OrderFormResponse. */
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
  // Item's department for PER_DEPARTMENT clients (the form groups by it); null for COMBINED.
  departmentId: number | null
  // The order-form payload omits price (resolved server-side at creation). The authenticated
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
  departmentId: number | null   // item's department snapshot (PER_DEPARTMENT clients only)
}

/** Full order detail — mirrors OrderResponse. */
export interface Order {
  id:                number
  orderNumber:       string
  clientId:          number
  orderDate:         string   // ISO date
  dueDate:           string | null
  status:            OrderStatus
  pricingMultiplier: number
  submittedByName:   string | null
  notes:             string | null
  createdByUserId:   number | null
  createdAt:         string   // ISO instant
  total:             number
  lineItems:         OrderLineItem[]
}

/** Lightweight list row — mirrors OrderSummaryResponse. */
export interface OrderSummary {
  id:                number
  orderNumber:       string
  clientId:          number
  orderDate:         string
  dueDate:           string | null
  status:            OrderStatus
  pricingMultiplier: number
  submittedByName:   string | null
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

/** An open-pool order as a driver sees it — mirrors DriverDeliveryResponse. No prices. */
export interface DriverDelivery {
  orderId:        number
  orderNumber:    string
  clientName:     string
  orderDate:      string
  dueDate:        string | null
  status:         OrderStatus
  notes:          string | null
  lines:          DriverDeliveryLine[]
}

// ── Billing (Phase 3) ──────────────────────────────────────────────────────────

/** One-way lifecycle: DRAFT → ISSUED → PAID. DRAFT may be regenerated; ISSUED/PAID are locked. */
export type BillingStatus = 'DRAFT' | 'ISSUED' | 'PAID'

/** One order on a monthly billing — mirrors MonthlyBillingLineResponse. */
export interface MonthlyBillingLine {
  orderId:     number
  orderNumber: string
  orderDate:   string   // ISO date
  subtotal:    number
}

/** Monthly billing detail — mirrors MonthlyBillingResponse. */
export interface MonthlyBilling {
  id:             number
  billingNumber:  string
  clientId:       number
  departmentId:   number | null
  departmentName: string | null   // denormalized for display (PER_DEPARTMENT clients)
  periodYear:     number
  periodMonth:   number   // 1-12
  invoiceDate:   string   // ISO date
  total:         number
  status:        BillingStatus
  hasPdf:        boolean
  notes:         string | null
  lines:         MonthlyBillingLine[]
}

/** Per-order invoice metadata + a short-lived pre-signed PDF URL — mirrors OrderInvoiceResponse. */
export interface OrderInvoice {
  invoiceNumber: string
  orderId:       number
  invoiceDate:   string   // ISO date
  subtotal:      number
  pdfUrl:        string
}

// ── Dashboard & Reports (Phase 4) ──────────────────────────────────────────────
// All money is "billable orders by order date" (every non-canceled order, multiplier-inclusive).

/** Dashboard summary cards — mirrors DashboardSummaryResponse. */
export interface DashboardSummary {
  ordersToday:      number
  inProgress:       number
  readyForDelivery: number
  revenueThisMonth: number
}

/** One month on the FINANCE_STAFF dashboard trend — mirrors FinanceTrendResponse. `month` is "YYYY-MM". */
export interface FinanceTrendPoint {
  month:      string
  revenue:    number
  orderCount: number
}

/** Owner analytics dashboard payload — mirrors DashboardAnalyticsResponse. `month` is "YYYY-MM". */
export interface DashboardAnalytics {
  from:           string
  to:             string
  totalRevenue:   number
  totalOrders:    number
  avgOrderValue:  number
  bestMonth:      { month: string; revenue: number } | null
  hotels:         AnalyticsHotelTotal[]   // ranked by revenue desc; drives legend + color order
  months:         AnalyticsMonthPoint[]   // chronological
}

export interface AnalyticsHotelTotal {
  clientId:   number
  name:       string
  code:       string | null
  orderCount: number
  revenue:    number
}

export interface AnalyticsMonthPoint {
  month:    string   // "YYYY-MM"
  revenue:  number
  partial:  boolean
  perHotel: { clientId: number; revenue: number }[]
}

/** Per-client billable totals for a report period — mirrors ClientLineResponse. */
export interface ReportClientLine {
  clientId:   number
  clientName: string
  clientCode: string | null
  orderCount: number
  total:      number
}

/** Daily summary — mirrors DailyReportResponse. */
export interface DailyReport {
  date:       string   // ISO date
  clients:    ReportClientLine[]
  grandTotal: number
}

/** Monthly per-client report — mirrors MonthlyReportResponse. */
export interface MonthlyReport {
  year:       number
  month:      number   // 1-12
  clients:    ReportClientLine[]
  grandTotal: number
}

/** Per-hotel report over a date range — mirrors HotelReportResponse. */
export interface HotelReport {
  clientId:   number
  clientName: string
  clientCode: string | null
  from:       string   // ISO date
  to:         string   // ISO date
  orders:     HotelReportOrderLine[]
  items:      HotelReportItemLine[]
  grandTotal: number
}

export interface HotelReportOrderLine {
  orderId:     number
  orderNumber: string
  orderDate:   string   // ISO date
  status:      OrderStatus
  total:       number
}

export interface HotelReportItemLine {
  itemName: string
  unit:     string | null
  quantity: number
  total:    number
}
