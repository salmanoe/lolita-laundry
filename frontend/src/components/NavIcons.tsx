// Minimalist line icons for the sidebar nav — white stroke (currentColor), laundry-themed,
// matching the reference set's thin-line / rounded-cap style. 24×24, fill: none.

type IconProps = { className?: string }

const base = {
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.7,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
}

/** Dasbor — house (home). */
export function HomeIcon({ className }: IconProps) {
  return (
    <svg className={className} {...base}>
      <path d="M3 10.7 12 4l9 6.7" />
      <path d="M5.5 9.4V19a1 1 0 0 0 1 1h11a1 1 0 0 0 1-1V9.4" />
      <path d="M9.7 20v-5a1 1 0 0 1 1-1h2.6a1 1 0 0 1 1 1v5" />
    </svg>
  )
}

/** Klien — hotel/building. */
export function HotelIcon({ className }: IconProps) {
  return (
    <svg className={className} {...base}>
      <rect x="5" y="3" width="14" height="18" rx="1.2" />
      <path d="M9 7.5h1.5M13.5 7.5H15M9 11h1.5M13.5 11H15M9 14.5h1.5M13.5 14.5H15" />
      <path d="M10.3 21v-2.4a1.7 1.7 0 0 1 3.4 0V21" />
    </svg>
  )
}

/** Order — laundry basket. */
export function BasketIcon({ className }: IconProps) {
  return (
    <svg className={className} {...base}>
      <path d="M4.5 9h15" />
      <path d="M6 9l1 10.4a1.5 1.5 0 0 0 1.5 1.35h7a1.5 1.5 0 0 0 1.5-1.35L18 9" />
      <path d="M8.6 9l1.15-3.8a1 1 0 0 1 .96-.7h2.58a1 1 0 0 1 .96.7L15.4 9" />
      <path d="M9.3 12.5l.45 5M12 12.5v5M14.7 12.5l-.45 5" />
    </svg>
  )
}

/** Item — folded towels / linens stack. */
export function TowelsIcon({ className }: IconProps) {
  return (
    <svg className={className} {...base}>
      <rect x="4.5" y="6" width="15" height="3.5" rx="1.2" />
      <rect x="4.5" y="10.25" width="15" height="3.5" rx="1.2" />
      <rect x="4.5" y="14.5" width="15" height="3.5" rx="1.2" />
      <path d="M7.6 6v3.5M7.6 10.25v3.5M7.6 14.5v3.5" />
    </svg>
  )
}

/** Tagihan — invoice / document with lines. */
export function InvoiceIcon({ className }: IconProps) {
  return (
    <svg className={className} {...base}>
      <path d="M7 3h7l4 4v12.5a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z" />
      <path d="M13.5 3v4h4" />
      <path d="M9 12h6M9 15h6M9 9h2" />
    </svg>
  )
}

/** Laporan — bar chart (reports / analytics). */
export function ChartIcon({ className }: IconProps) {
  return (
    <svg className={className} {...base}>
      <path d="M4 4v15a1 1 0 0 0 1 1h15" />
      <path d="M8 16v-3M12 16v-6M16 16v-9" />
    </svg>
  )
}

/** Master Data — sliders (adjust reference data). */
export function SlidersIcon({ className }: IconProps) {
  return (
    <svg className={className} {...base}>
      <path d="M4 7h9M19 7h1" />
      <circle cx="16" cy="7" r="2" />
      <path d="M4 12h2M12 12h8" />
      <circle cx="9" cy="12" r="2" />
      <path d="M4 17h9M19 17h1" />
      <circle cx="16" cy="17" r="2" />
    </svg>
  )
}
