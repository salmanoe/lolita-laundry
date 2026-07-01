import { useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { ApiError, apiFetch, asArray } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import OrderItemPicker, { toLineItems, type QuantityMap } from '../components/OrderItemPicker'
import type { ClientOption, Order, OrderForm } from '../types/api'

/**
 * In-house order entry for DAILY_STAFF (and SUPER_ADMIN). Replaces the retired public tokenized
 * form: staff pick the hotel from a dropdown, the form loads that client's priced items (grouped
 * by department for PER_DEPARTMENT clients), and submits to POST /api/orders. Prices are resolved
 * server-side, so the picker stays price-free here.
 */
export default function NewOrderPage() {
  const { getAccessTokenSilently } = useAuth()
  const token = () => getAccessTokenSilently()

  const [clientId, setClientId] = useState<number | ''>('')
  const [submittedByName, setSubmittedByName] = useState('')
  const [treatment, setTreatment] = useState(false)
  const [notes, setNotes] = useState('')
  const [quantities, setQuantities] = useState<QuantityMap>({})
  const [confirmed, setConfirmed] = useState<Order | null>(null)

  const clientsQ = useQuery({
    queryKey: ['clients', 'options'],
    queryFn: async () => apiFetch<ClientOption[]>('/api/clients/options', { token: await token() }),
  })
  const clients = asArray<ClientOption>(clientsQ.data)

  const formQ = useQuery({
    queryKey: ['order-form', clientId],
    enabled: clientId !== '',
    queryFn: async () => apiFetch<OrderForm>(`/api/orders/form?clientId=${clientId}`, { token: await token() }),
  })

  const form = formQ.data

  const departmentName = useMemo(() => {
    const map = new Map(form?.departments.map((d) => [d.id, d.name]))
    return (id: number) => map.get(id) ?? '—'
  }, [form])
  const unitName = useMemo(() => {
    const map = new Map(form?.items.map((i) => [i.unitId, i.unitName ?? '—']))
    return (id: number) => map.get(id) ?? '—'
  }, [form])

  const lines = toLineItems(quantities)

  const submit = useMutation({
    mutationFn: async () =>
      apiFetch<Order>('/api/orders', {
        method: 'POST',
        token: await token(),
        body: JSON.stringify({
          clientId,
          treatment,
          dueDate: null,
          submittedByName: submittedByName.trim(),
          notes: notes.trim() || null,
          items: lines,
        }),
      }),
    onSuccess: (order) => setConfirmed(order),
  })

  function resetForm() {
    setConfirmed(null)
    setQuantities({})
    setNotes('')
    setTreatment(false)
    setSubmittedByName('')
    setClientId('')
  }

  // When the hotel changes, clear the in-progress item selections (they belong to the old client).
  function changeClient(value: string) {
    setClientId(value === '' ? '' : Number(value))
    setQuantities({})
    setTreatment(false)
  }

  const canSubmit =
    clientId !== '' && submittedByName.trim().length > 0 && lines.length > 0 && !submit.isPending

  if (confirmed) {
    const clientName = clients.find((c) => c.id === clientId)?.name ?? form?.clientName
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-green-100 text-2xl">
            ✓
          </div>
          <h1 className="text-lg font-semibold text-gray-800">Order terkirim!</h1>
          <p className="mt-1 text-sm text-gray-500">
            Nomor order untuk <span className="font-medium">{clientName}</span>:
          </p>
          <p className="mt-2 font-mono text-lg font-bold text-brand-700">{confirmed.orderNumber}</p>
          <button
            onClick={resetForm}
            className="mt-6 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
          >
            Buat Order Baru
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6 pb-32">
      <h1 className="text-lg font-semibold text-gray-800">Buat Order</h1>

      {/* Hotel + submitter + options */}
      <div className="space-y-4 rounded-lg border bg-white p-4">
        <Field label="Hotel / Klien" required>
          <select value={clientId} onChange={(e) => changeClient(e.target.value)} className={inputCls}>
            <option value="">— Pilih hotel —</option>
            {clients.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </Field>

        <Field label="Nama Staff" required>
          <input
            value={submittedByName}
            onChange={(e) => setSubmittedByName(e.target.value)}
            placeholder="Nama Anda"
            className={inputCls}
          />
        </Field>

        {form?.treatmentAvailable && (
          <div>
            <label className="flex items-center gap-2 text-sm text-gray-700">
              <input
                type="checkbox"
                checked={treatment}
                onChange={(e) => setTreatment(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-brand-600 focus:ring-brand-500"
              />
              Treatment (harga ×2)
            </label>
            <p
              className={`mt-1 text-xs ${
                treatment ? 'rounded-md bg-amber-50 px-2 py-1 text-amber-700' : 'text-gray-400'
              }`}
            >
              Treatment berlaku untuk <strong>semua item</strong> di order ini. Untuk item Reguler, buat order terpisah.
            </p>
          </div>
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
        {clientId === '' ? (
          <p className="rounded-lg border bg-white px-4 py-6 text-center text-sm text-gray-400">
            Pilih hotel terlebih dahulu untuk menampilkan item.
          </p>
        ) : formQ.isLoading ? (
          <p className="rounded-lg border bg-white px-4 py-6 text-center text-sm text-gray-400">Memuat item…</p>
        ) : formQ.error ? (
          <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">Gagal memuat item untuk hotel ini.</p>
        ) : form ? (
          <OrderItemPicker
            items={form.items}
            departmentName={departmentName}
            unitName={unitName}
            showPrices={false}
            quantities={quantities}
            onChange={setQuantities}
          />
        ) : null}
      </div>

      {submit.error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
          {submit.error instanceof ApiError ? submit.error.detail : 'Gagal mengirim order.'}
        </p>
      )}

      {/* Sticky submit bar */}
      <div className="fixed inset-x-0 bottom-0 border-t bg-white/95 backdrop-blur">
        <div className="mx-auto flex w-full max-w-2xl items-center justify-between gap-4 px-4 py-3">
          <p className="text-sm font-medium text-gray-600">{lines.length} item dipilih</p>
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
