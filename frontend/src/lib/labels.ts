// Static label for the one enum that stays fixed in code (BillingMode drives billing logic).
// Item units, item categories and client types are dynamic reference data — resolve their
// labels from the lookup lists (see lib/lookups.ts), not from here.
import type { BillingMode } from '../types/api'

export const billingModeLabel: Record<BillingMode, string> = {
  COMBINED: 'Gabungan',
  PER_DEPARTMENT: 'Per Departemen',
}