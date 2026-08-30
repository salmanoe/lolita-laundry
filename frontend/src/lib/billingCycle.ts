/**
 * Client-side mirror of the backend `billing/domain/BillingCycle`.
 *
 * A client's billing period is a plain calendar month by default (`cutoffDay === null`), or a
 * cut-off cycle: cut-off `D` means the period runs from `D+1` of the previous month through `D`,
 * and is labelled by the month it **ends** in (26 Jul – 25 Aug 2026 is the "2026-08" period).
 *
 * Keep this in step with the Java value object — the two must agree on period boundaries.
 */

const iso = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

/** The billing period a date falls into, as a 1-indexed { year, month }. */
export function billingPeriodOf(date: Date, cutoffDay: number | null) {
  const rollsForward = cutoffDay != null && date.getDate() > cutoffDay
  const d = new Date(date.getFullYear(), date.getMonth() + (rollsForward ? 1 : 0), 1)
  return { year: d.getFullYear(), month: d.getMonth() + 1 }
}

/** The first date a billing period covers, as an ISO date string. */
export function billingPeriodStart(year: number, month: number, cutoffDay: number | null): string {
  if (cutoffDay == null) return iso(new Date(year, month - 1, 1))
  // The day after the cut-off in the previous month. `cutoffDay + 1` can overflow a short
  // February (cut-off 28 → 29 Feb); Date normalises that to 1 March, which is exactly what the
  // backend's `atDay(cutoffDay).plusDays(1)` produces.
  return iso(new Date(year, month - 2, cutoffDay + 1))
}

/**
 * The start of the billing period **before** the one `today` falls in.
 *
 * This is the default re-sync window: it always lands on a period boundary, so a re-sync can
 * never begin mid-period and leave that period's earlier orders un-re-homed.
 */
export function previousPeriodStart(today: Date, cutoffDay: number | null): string {
  const current = billingPeriodOf(today, cutoffDay)
  const previous = new Date(current.year, current.month - 2, 1) // normalises a January underflow
  return billingPeriodStart(previous.getFullYear(), previous.getMonth() + 1, cutoffDay)
}
