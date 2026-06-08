import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { monthLabelFromYm } from '../../lib/labels'
import { rupiah, rupiahCompact } from '../../lib/money'
import { MAX_BAR_SERIES, OTHERS_COLOR, colorFor } from '../../lib/clientColors'
import type { AnalyticsHotelTotal, AnalyticsMonthPoint } from '../../types/api'

/**
 * Per-hotel monthly revenue, stacked. Hotels beyond the top {@link MAX_BAR_SERIES} (by overall
 * revenue) collapse into a single "Lainnya" series so the chart stays legible.
 */
export default function MonthlyBreakdownChart({
  hotels,
  months,
}: {
  hotels: AnalyticsHotelTotal[]
  months: AnalyticsMonthPoint[]
}) {
  const shown = hotels.slice(0, MAX_BAR_SERIES)
  const others = hotels.slice(MAX_BAR_SERIES)
  const hasOthers = others.length > 0
  const otherIds = new Set(others.map((h) => h.clientId))

  const data = months.map((m) => {
    const byClient = new Map(m.perHotel.map((s) => [s.clientId, s.revenue]))
    const row: Record<string, number | string> = {
      label: monthLabelFromYm(m.month, true) + (m.partial ? '*' : ''),
    }
    shown.forEach((h) => {
      row[String(h.clientId)] = byClient.get(h.clientId) ?? 0
    })
    if (hasOthers) {
      let sum = 0
      m.perHotel.forEach((s) => {
        if (otherIds.has(s.clientId)) sum += s.revenue
      })
      row.others = sum
    }
    return row
  })

  return (
    <ResponsiveContainer width="100%" height={340}>
      <BarChart data={data} margin={{ top: 8, right: 12, bottom: 0, left: 4 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#eef2f7" vertical={false} />
        <XAxis dataKey="label" tick={{ fontSize: 12, fill: '#6b7280' }} tickLine={false} axisLine={{ stroke: '#e5e7eb' }} />
        <YAxis tickFormatter={rupiahCompact} tick={{ fontSize: 11, fill: '#9ca3af' }} tickLine={false} axisLine={false} width={72} />
        <Tooltip formatter={(v, name) => [rupiah(Number(v)), String(name)]} />
        <Legend wrapperStyle={{ fontSize: 12, paddingTop: 8 }} />
        {shown.map((h, i) => (
          <Bar key={h.clientId} dataKey={String(h.clientId)} stackId="a" name={h.name} fill={colorFor(i)} maxBarSize={48} />
        ))}
        {hasOthers && <Bar dataKey="others" stackId="a" name="Lainnya" fill={OTHERS_COLOR} maxBarSize={48} />}
      </BarChart>
    </ResponsiveContainer>
  )
}