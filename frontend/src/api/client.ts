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

  // Let the browser set Content-Type (incl. the multipart boundary) for FormData bodies.
  const isFormData = typeof FormData !== 'undefined' && rest.body instanceof FormData

  const response = await fetch(`${BASE}${path}`, {
    ...rest,
    headers: {
      // Only declare a JSON body when one is actually sent (skip on GET/DELETE and multipart)
      ...(rest.body && !isFormData ? { 'Content-Type': 'application/json' } : {}),
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

/**
 * Coerce a list endpoint's response into an array.
 *
 * A `List<T>` endpoint should serialize to a JSON array, but a misconfigured backend/proxy can
 * hand back a Spring `Page` wrapper (`{ content: [...] }`) or some other object. A non-array
 * slips past `?? []` (which only catches null/undefined) and then blows up the whole page at
 * `.map`/`.find`. This guard recovers a `{ content }` page shape and otherwise degrades to `[]`.
 */
export function asArray<T>(data: unknown): T[] {
  if (Array.isArray(data)) return data as T[]
  if (data && typeof data === 'object' && Array.isArray((data as { content?: unknown }).content)) {
    return (data as { content: T[] }).content
  }
  return []
}
