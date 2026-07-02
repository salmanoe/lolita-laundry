import { ApiError } from '../api/client'

const BASE = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * Downloads a file from an authenticated endpoint as a browser save. `apiFetch` only handles JSON,
 * so this fetches the body as a blob and triggers a download via a temporary object URL. The
 * filename comes from the Content-Disposition header (exposed via CORS), falling back to
 * `fallbackName` when the header is unreadable.
 */
export async function downloadAuthed(path: string, token: string, fallbackName: string): Promise<void> {
  const res = await fetch(`${BASE}${path}`, { headers: { Authorization: `Bearer ${token}` } })
  if (!res.ok) {
    const body = await res.json().catch(() => ({ detail: res.statusText }))
    throw new ApiError(res.status, body.detail ?? res.statusText)
  }

  const blob = await res.blob()
  const name = filenameFromDisposition(res.headers.get('Content-Disposition')) ?? fallbackName

  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

/**
 * Triggers a browser save of a URL that already carries `Content-Disposition: attachment`
 * (our pre-signed PDF download URLs do). We deliberately omit the `download` attribute — it is
 * ignored for cross-origin URLs anyway — and rely on the server-sent disposition, which downloads
 * reliably on mobile browsers where an inline PDF tab is flaky.
 */
export function openDownloadUrl(url: string): void {
  const a = document.createElement('a')
  a.href = url
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  a.remove()
}

function filenameFromDisposition(value: string | null): string | undefined {
  if (!value) return undefined
  // RFC 5987 form: filename*=UTF-8''Laporan-....xlsx
  const star = /filename\*=(?:UTF-8'')?([^;]+)/i.exec(value)
  if (star) return decodeURIComponent(star[1].replace(/"/g, '').trim())
  const plain = /filename="?([^";]+)"?/i.exec(value)
  return plain ? plain[1].trim() : undefined
}