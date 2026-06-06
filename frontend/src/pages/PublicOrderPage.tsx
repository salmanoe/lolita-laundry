import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { ApiError, apiFetch } from '../api/client'
import OrderItemPicker, { toLineItems, type QuantityMap } from '../components/OrderItemPicker'
import type { Order, OrderForm } from '../types/api'

/**
 * Public, tokenized order form. Hotel staff reach this via /order/{token} — no login.
 * Mobile-first: they fill it on a phone. Submits to POST /public/orders/{token}.
 */
export default function PublicOrderPage() {
  const { token } = useParams<{ token: string }>()

  const formQ = useQuery({
    queryKey: ['public-order-form', token],
    queryFn: () => apiFetch<OrderForm>(`/public/order-form/${token}`),
    retry: false,
  })

  const [submittedByName, setSubmittedByName] = useState('')
  const [departmentId, setDepartmentId] = useState<number | ''>('')
  const [treatment, setTreatment] = useState(false)
  const [notes, setNotes] = useState('')
  const [quantities, setQuantities] = useState<QuantityMap>({})
  const [confirmed, setConfirmed] = useState<Order | null>(null)

  const form = formQ.data

  // id → name resolvers for the picker (names are now carried in the public payload).
  const categoryName = useMemo(() => {
    const map = new Map(form?.items.map((i) => [i.categoryId, i.categoryName ?? '—']))
    return (id: number) => map.get(id) ?? '—'
  }, [form])
  const unitName = useMemo(() => {
    const map = new Map(form?.items.map((i) => [i.unitId, i.unitName ?? '—']))
    return (id: number) => map.get(id) ?? '—'
  }, [form])

  const lines = toLineItems(quantities)

  const submit = useMutation({
    mutationFn: () =>
      apiFetch<Order>(`/public/orders/${token}`, {
        method: 'POST',
        body: JSON.stringify({
          submittedByName: submittedByName.trim(),
          departmentId: departmentId === '' ? null : departmentId,
          treatment,
          notes: notes.trim() || null,
          items: lines,
        }),
      }),
    onSuccess: (order) => setConfirmed(order),
  })

  const departmentMissing = !!form?.perDepartment && departmentId === ''
  const canSubmit =
    submittedByName.trim().length > 0 && lines.length > 0 && !departmentMissing && !submit.isPending

  if (formQ.isLoading) {
    return <Centered>Memuat formulir order…</Centered>
  }
  if (formQ.error) {
    const msg =
      formQ.error instanceof ApiError && formQ.error.status === 400
        ? 'Tautan order tidak valid atau sudah tidak aktif.'
        : 'Gagal memuat formulir order. Coba lagi nanti.'
    return <Centered tone="error">{msg}</Centered>
  }
  if (!form) return null

  if (confirmed) {
    return (
      <Centered>
        <div className="text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-green-100 text-2xl">
            ✓
          </div>
          <h1 className="text-lg font-semibold text-gray-800">Order terkirim!</h1>
          <p className="mt-1 text-sm text-gray-500">
            Nomor order Anda untuk <span className="font-medium">{form.clientName}</span>:
          </p>
          <p className="mt-2 font-mono text-lg font-bold text-brand-700">{confirmed.orderNumber}</p>
          <button
            onClick={() => {
              setConfirmed(null)
              setQuantities({})
              setNotes('')
              setTreatment(false)
            }}
            className="mt-6 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
          >
            Buat Order Baru
          </button>
        </div>
      </Centered>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50 pb-32">
      <header className="bg-gradient-to-r from-brand-800 via-brand-700 to-brand-500 text-white shadow-sm">
        <div className="mx-auto flex max-w-2xl items-center gap-3 px-4 py-4">
          <img src="/logo-white.png" alt="Lolita Laundry" className="h-10 w-auto" />
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-white">{form.clientName}</p>
            <p className="font-mono text-xs text-blue-100/80">{form.clientCode}</p>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-2xl space-y-6 px-4 py-6">
        <h1 className="text-lg font-semibold text-gray-800">Formulir Order Laundry</h1>

        {/* Submitter + options */}
        <div className="space-y-4 rounded-lg border bg-white p-4">
          <Field label="Nama Staff" required>
            <input
              value={submittedByName}
              onChange={(e) => setSubmittedByName(e.target.value)}
              placeholder="Nama Anda"
              className={inputCls}
            />
          </Field>

          {form.perDepartment && (
            <Field label="Departemen" required>
              <select
                value={departmentId}
                onChange={(e) => setDepartmentId(e.target.value === '' ? '' : Number(e.target.value))}
                className={inputCls}
              >
                <option value="">— Pilih departemen —</option>
                {form.departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </Field>
          )}

          {form.treatmentAvailable && (
            <label className="flex items-center gap-2 text-sm text-gray-700">
              <input
                type="checkbox"
                checked={treatment}
                onChange={(e) => setTreatment(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-brand-600 focus:ring-brand-500"
              />
              Treatment (harga ×2)
            </label>
          )}

          <Field label="Catatan">
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              placeholder="Opsional"
              className={inputCls}
            />
          </Field>
        </div>

        {/* Items */}
        <div>
          <h2 className="mb-2 text-sm font-semibold text-gray-700">Item</h2>
          <OrderItemPicker
            items={form.items}
            categoryName={categoryName}
            unitName={unitName}
            showPrices={false}
            quantities={quantities}
            onChange={setQuantities}
          />
        </div>

        {submit.error && (
          <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
            {submit.error instanceof ApiError ? submit.error.detail : 'Gagal mengirim order.'}
          </p>
        )}
      </main>

      {/* Sticky submit bar with running total */}
      <div className="fixed inset-x-0 bottom-0 border-t bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-2xl items-center justify-between gap-4 px-4 py-3">
          <p className="text-sm font-medium text-gray-600">
            {lines.length} item dipilih
          </p>
          <button
            onClick={() => submit.mutate()}
            disabled={!canSubmit}
            className="rounded-lg bg-brand-600 px-6 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {submit.isPending ? 'Mengirim…' : 'Kirim Order'}
          </button>
        </div>
      </div>
    </div>
  )
}

const inputCls =
  'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'

function Field({ label, required, children }: { label: string; required?: boolean; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs font-medium text-gray-500">
        {label} {required && <span className="text-red-500">*</span>}
      </span>
      {children}
    </label>
  )
}

function Centered({ children, tone }: { children: React.ReactNode; tone?: 'error' }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 p-4">
      <div className={`max-w-sm text-sm ${tone === 'error' ? 'text-red-500' : 'text-gray-500'}`}>{children}</div>
    </div>
  )
}
