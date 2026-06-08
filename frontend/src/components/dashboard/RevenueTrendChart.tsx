import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { monthLabelFromYm } from '../../lib/labels'
import { rupiah, rupiahCompact } from '../../lib/money'
import type { AnalyticsMonthPoint } from '../../types/api'

/** Monthly revenue trend (area line). The current (partial) month is marked with a trailing *. */
export default function RevenueTrendChart({ months }: { months: AnalyticsMonthPoint[] }) {
  const data = months.map((m) => ({
    label: monthLabelFromYm(m.month, true) + (m.partial ? '*' : ''),
    revenue: m.revenue,
  }))

  return (
    <ResponsiveContainer width="100%" height={260}>
      <AreaChart data={data} margin={{ top: 8, right: 12, bottom: 0, left: 4 }}>
        <defs>
          <linearGradient id="revFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#2563eb" stopOpacity={0.25} />
            <stop offset="100%" stopColor="#2563eb" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="#eef2f7" vertical={false} />
        <XAxis dataKey="label" tick={{ fontSize: 12, fill: '#6b7280' }} tickLine={false} axisLine={{ stroke: '#e5e7eb' }} />
        <YAxis tickFormatter={rupiahCompact} tick={{ fontSize: 11, fill: '#9ca3af' }} tickLine={false} axisLine={false} width={72} />
        <Tooltip formatter={(v) => [rupiah(Number(v)), 'Pendapatan']} />
        <Area
          type="monotone"
          dataKey="revenue"
          stroke="#2563eb"
          strokeWidth={2}
          fill="url(#revFill)"
          dot={{ r: 3, fill: '#2563eb' }}
          activeDot={{ r: 5 }}
        />
      </AreaChart>
    </ResponsiveContainer>
  )
}
