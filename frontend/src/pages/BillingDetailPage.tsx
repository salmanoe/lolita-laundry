import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { openDownloadUrl } from '../lib/download'
import { billingStatusBadge, billingStatusLabel, monthName, nextBillingStatus } from '../lib/labels'
import type { Client, MonthlyBilling } from '../types/api'

const rupiah = (n: number) =>
  new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)

export default function BillingDetailPage() {
  const { id } = useParams()
  const billingId = Number(id)
  const { getAccessTokenSilently } = useAuth()
  const qc = useQueryClient()
  const token = async () => getAccessTokenSilently()

  const billingQ = useQuery({
    queryKey: ['billing', billingId],
    queryFn: async () => apiFetch<MonthlyBilling>(`/api/billing/${billingId}`, { token: await token() }),
  })
  const billing = billingQ.data

  const clientQ = useQuery({
    queryKey: ['client', billing?.clientId],
    enabled: !!billing,
    queryFn: async () => apiFetch<Client>(`/api/clients/${billing!.clientId}`, { token: await token() }),
  })
  const advance = useMutation({
    mutationFn: async (status: string) =>
      apiFetch<MonthlyBilling>(`/api/billing/${billingId}/status`, {
        method: 'PATCH',
        token: await token(),
        body: JSON.stringify({ status }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['billing', billingId] })
      qc.invalidateQueries({ queryKey: ['billings'] })
    },
  })

  // Opens the PDF in a new tab via a fresh short-lived pre-signed URL.
  const openPdf = useMutation({
    mutationFn: async () =>
      apiFetch<{ url: string }>(`/api/billing/${billingId}/pdf`, { token: await token() }),
    onSuccess: ({ url }) => window.open(url, '_blank', 'noopener'),
  })

  // "Unduh": a download-disposition URL — reliable on mobile where inline preview is flaky.
  const downloadPdf = useMutation({
    mutationFn: async () =>
      apiFetch<{ url: string }>(`/api/billing/${billingId}/pdf?download=true`, { token: await token() }),
    onSuccess: ({ url }) => openDownloadUrl(url),
  })

  if (billingQ.isLoading) return <div className="text-sm text-gray-400">Memuat tagihan...</div>
  if (billingQ.error || !billing) return <div className="text-sm text-red-500">Gagal memuat tagihan.</div>

  const deptName = billing.departmentName
  const next = nextBillingStatus[billing.status]

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <Link to="/billing" className="text-sm text-gray-500 hover:text-gray-700">← Tagihan</Link>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="font-mono text-xl font-semibold text-gray-800">{billing.billingNumber}</h1>
            <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${billingStatusBadge[billing.status]}`}>
              {billingStatusLabel[billing.status]}
            </span>
          </div>
          <div className="flex gap-2">
            {billing.hasPdf && (
              <>
                <button
                  onClick={() => openPdf.mutate()}
                  disabled={openPdf.isPending}
                  className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                >
                  {openPdf.isPending ? 'Membuka…' : 'Lihat PDF'}
                </button>
                <button
                  onClick={() => downloadPdf.mutate()}
                  disabled={downloadPdf.isPending}
                  className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                >
                  {downloadPdf.isPending ? 'Mengunduh…' : 'Unduh'}
                </button>
              </>
            )}
            {next && (
              <button
                onClick={() => advance.mutate(next)}
                disabled={advance.isPending}
                className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-50"
              >
                {advance.isPending ? 'Memproses…' : `Tandai ${billingStatusLabel[next]}`}
              </button>
            )}
          </div>
        </div>
        {advance.error && (
          <p className="mt-2 text-sm text-red-500">
            {advance.error instanceof ApiError ? advance.error.detail : 'Gagal mengubah status.'}
          </p>
        )}
        {openPdf.error && (
          <p className="mt-2 text-sm text-red-500">
            {openPdf.error instanceof ApiError ? openPdf.error.detail : 'Gagal membuka PDF.'}
          </p>
        )}
        {downloadPdf.error && (
          <p className="mt-2 text-sm text-red-500">
            {downloadPdf.error instanceof ApiError ? downloadPdf.error.detail : 'Gagal mengunduh PDF.'}
          </p>
        )}
      </div>

      {/* Info */}
      <dl className="grid grid-cols-2 gap-x-6 gap-y-4 rounded-lg border bg-white p-6 text-sm shadow-sm md:grid-cols-4">
        <Info label="Klien" value={clientQ.data?.name ?? '—'} />
        {billing.departmentId && <Info label="Departemen" value={deptName ?? '—'} />}
        <Info label="Periode" value={`${monthName[billing.periodMonth]} ${billing.periodYear}`} />
        <Info label="Tanggal Tagihan" value={billing.invoiceDate} />
        <Info label="Total" value={<span className="font-semibold">{rupiah(billing.total)}</span>} />
        {billing.notes && <Info label="Catatan" value={billing.notes} />}
      </dl>

      {/* Lines — one per order */}
      <section>
        <h2 className="mb-3 text-base font-semibold text-gray-800">Order ({billing.lines.length})</h2>
        <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['No. Order', 'Tanggal', 'Subtotal'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {billing.lines.map((l) => (
                <tr key={l.orderId} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <Link to={`/orders/${l.orderId}`} className="font-mono font-medium text-brand-700 hover:underline">
                      {l.orderNumber}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-gray-500">{l.orderDate}</td>
                  <td className="px-4 py-3 font-medium text-gray-700">{rupiah(l.subtotal)}</td>
                </tr>
              ))}
              {billing.lines.length === 0 && (
                <tr>
                  <td colSpan={3} className="px-4 py-8 text-center text-sm text-gray-400">
                    Tidak ada order pada tagihan ini.
                  </td>
                </tr>
              )}
            </tbody>
            <tfoot>
              <tr className="border-t bg-gray-50">
                <td colSpan={2} className="px-4 py-3 text-right font-medium text-gray-600">Total</td>
                <td className="px-4 py-3 font-bold text-gray-800">{rupiah(billing.total)}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>
    </div>
  )
}

function Info({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs font-medium text-gray-400">{label}</dt>
      <dd className="mt-0.5 text-gray-800">{value}</dd>
    </div>
  )
}
