/**
 * Typed API client for the Lolita Laundry backend.
 *
 * Usage inside a TanStack Query queryFn (authenticated):
 *   queryFn: async () => apiFetch<Client[]>('/api/clients', { token })
 *
 * Usage for public routes (no Auth0 token required):
 *   apiFetch<OrderForm>('/public/order-form/abc123')
 */

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly detail: string,
  ) {
    super(detail)
    this.name = 'ApiError'
  }
}

interface FetchOptions extends Omit<RequestInit, 'headers'> {
  /**
   * Auth0 access token. Required for all /api/* requests.
   * Omit for public routes (/public/**) — the Authorization header is not sent.
   */
  token?: string
  headers?: Record<string, string>
}

const BASE = import.meta.env.VITE_API_BASE_URL ?? ''

export async function apiFetch<T>(path: string, options: FetchOptions = {}): Promise<T> {
  const { token, headers, ...rest } = options

  const response = await fetch(`${BASE}${path}`, {
    ...rest,
    headers: {
      // Only declare a JSON body when one is actually sent (skip on GET/DELETE)
      ...(rest.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
  })

  if (!response.ok) {
    const body = await response.json().catch(() => ({ detail: response.statusText }))
    throw new ApiError(response.status, body.detail ?? response.statusText)
  }

  // 204 No Content — return undefined cast to T
  if (response.status === 204) return undefined as T

  return response.json() as Promise<T>
}
