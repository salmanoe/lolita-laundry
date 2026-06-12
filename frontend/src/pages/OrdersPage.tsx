import { useState } from 'react'
import { Link } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useMe } from '../auth/useMe'
import Pagination from '../components/Pagination'
import { orderStatusBadge, orderStatusLabel } from '../lib/labels'
import type { ClientOption, OrderStatus, OrderSummary, Page } from '../types/api'

const SIZE = 10
const rupiah = (n: number) =>
  new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)

const STATUSES: OrderStatus[] = ['RECEIVED', 'PROCESSING', 'DONE', 'DELIVERED', 'CANCELLED']

export default function OrdersPage() {
  const { getAccessTokenSilently } = useAuth()
  // DAILY_STAFF are a price-free operator role — hide all monetary columns from them.
  const isDailyStaff = useMe().data?.role === 'DAILY_STAFF'
  const [page, setPage] = useState(0)
  const [clientId, setClientId] = useState<number | ''>('')
  const [status, setStatus] = useState<OrderStatus | ''>('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  // Client options for the filter dropdown and for resolving names in rows. Uses the lightweight
  // /options endpoint (readable by DAILY_STAFF too — the full client list is FINANCE_STAFF only).
  const clientsQ = useQuery({
    queryKey: ['clients', 'options'],
    queryFn: async () =>
      apiFetch<ClientOption[]>('/api/clients/options', { token: await getAccessTokenSilently() }),
  })
  const clientsById = new Map((clientsQ.data ?? []).map((c) => [c.id, c]))

  const params = new URLSearchParams({ page: String(page), size: String(SIZE) })
  if (clientId !== '') params.set('clientId', String(clientId))
  if (status !== '') params.set('status', status)
  if (from) params.set('from', from)
  if (to) params.set('to', to)

  const { data, isLoading, error } = useQuery({
    queryKey: ['orders', { page, clientId, status, from, to }],
    queryFn: async () =>
      apiFetch<Page<OrderSummary>>(`/api/orders?${params}`, { token: await getAccessTokenSilently() }),
    placeholderData: keepPreviousData,
  })

  function resetPageThen<T>(setter: (v: T) => void) {
    return (v: T) => {
      setPage(0)
      setter(v)
    }
  }

  const orders = data?.content ?? []

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-gray-800">Order</h1>
      </div>

      {/* Filters */}
      <div className="mb-4 grid grid-cols-2 gap-3 md:grid-cols-4">
        <select
          value={clientId}
          onChange={(e) => resetPageThen(setClientId)(e.target.value === '' ? '' : Number(e.target.value))}
          className={filterCls}
        >
          <option value="">Semua Klien</option>
          {(clientsQ.data ?? []).map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <select
          value={status}
          onChange={(e) => resetPageThen(setStatus)(e.target.value as OrderStatus | '')}
          className={filterCls}
        >
          <option value="">Semua Status</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {orderStatusLabel[s]}
            </option>
          ))}
        </select>
        <input
          type="date"
          value={from}
          onChange={(e) => resetPageThen(setFrom)(e.target.value)}
          className={filterCls}
          aria-label="Dari tanggal"
        />
        <input
          type="date"
          value={to}
          onChange={(e) => resetPageThen(setTo)(e.target.value)}
          className={filterCls}
          aria-label="Sampai tanggal"
        />
      </div>

      {isLoading ? (
        <div className="text-sm text-gray-400">Memuat data order...</div>
      ) : error ? (
        <div className="text-sm text-red-500">Gagal memuat data order.</div>
      ) : (
        <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['No. Order', 'Klien', 'Tanggal', 'Staff', ...(isDailyStaff ? [] : ['Total']), 'Status'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {orders.map((o) => (
                <tr key={o.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <Link to={`/orders/${o.id}`} className="font-mono font-medium text-brand-700 hover:underline">
                      {o.orderNumber}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-gray-700">{clientsById.get(o.clientId)?.name ?? `#${o.clientId}`}</td>
                  <td className="px-4 py-3 text-gray-500">{o.orderDate}</td>
                  <td className="px-4 py-3 text-gray-500">{o.submittedByName ?? '—'}</td>
                  {!isDailyStaff && <td className="px-4 py-3 font-medium text-gray-700">{rupiah(o.total)}</td>}
                  <td className="px-4 py-3">
                    <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${orderStatusBadge[o.status]}`}>
                      {orderStatusLabel[o.status]}
                    </span>
                  </td>
                </tr>
              ))}
              {orders.length === 0 && (
                <tr>
                  <td colSpan={isDailyStaff ? 5 : 6} className="px-4 py-8 text-center text-sm text-gray-400">
                    Belum ada order yang cocok dengan filter.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
          <Pagination
            page={data?.page ?? 0}
            totalPages={data?.totalPages ?? 0}
            totalElements={data?.totalElements ?? 0}
            onPage={setPage}
          />
        </div>
      )}
    </div>
  )
}

const filterCls =
  'rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'