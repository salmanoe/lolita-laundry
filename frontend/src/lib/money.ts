// Rupiah formatting. `rupiah` is full precision (tooltips, lists); `rupiahCompact` uses the
// Indonesian compact scale (rb / jt / M) for KPI cards and chart axes, e.g. "Rp 72,7 jt".

export const rupiah = (n: number) =>
  new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)

export const rupiahCompact = (n: number) =>
  new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(n)