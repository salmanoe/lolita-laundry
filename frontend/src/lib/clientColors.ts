// Fixed palette for per-hotel chart series. Colors are assigned by a hotel's rank index in the
// analytics payload's `hotels[]` (revenue desc), so the by-hotel list, the trend, and the stacked
// breakdown all agree for a given response.

const PALETTE = [
  '#2563eb', // blue
  '#16a34a', // green
  '#7c3aed', // violet
  '#ea580c', // orange
  '#d97706', // amber
  '#0891b2', // cyan
  '#db2777', // pink
  '#4f46e5', // indigo
]

/** Color for the hotel at the given rank index (wraps if there are more hotels than swatches). */
export function colorFor(index: number): string {
  return PALETTE[index % PALETTE.length]
}

/** Slate gray for the aggregated "Lainnya" series when more hotels than the bar-chart cap. */
export const OTHERS_COLOR = '#94a3b8'

/** Max distinct hotel series shown in the stacked bar chart before collapsing the rest into "Lainnya". */
export const MAX_BAR_SERIES = 6
