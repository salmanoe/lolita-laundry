// Tangkap screenshot Bab B (driver) & Bab C (staf).
// SYARAT: frontend berjalan di MOCK MODE (VITE_AUTH_MOCK=true) + backend profil dev.
// Strategi: intercept GET /api/me untuk menyetel role; endpoint lain diteruskan ke backend.
// Jalankan: node app.mjs
import { chromium } from 'playwright'
import { fileURLToPath } from 'url'
import path from 'path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const IMG = path.resolve(__dirname, '..', 'img')
const BASE = 'http://localhost:5173' // frontend mock-mode milik user
const API = 'http://localhost:8080'

const browser = await chromium.launch()

// Helper: buat context dengan role tertentu (me di-intercept).
async function withRole(role, viewport) {
  const ctx = await browser.newContext({ viewport, deviceScaleFactor: 2 })
  await ctx.route('**/api/me', (r) =>
    r.fulfill({ contentType: 'application/json', body: JSON.stringify({ id: 1, fullName: roleName(role), role }) }),
  )
  return ctx
}
const roleName = (r) => ({ DRIVER: 'Joko Pengantar', STAFF: 'Staf Lolita', OWNER: 'Pemilik Lolita' }[r] ?? 'User')
const shot = (page, name) => page.screenshot({ path: path.join(IMG, name + '.png') })
const settle = (page, ms = 700) => page.waitForTimeout(ms)

// ── BAB B — DRIVER (ponsel) ────────────────────────────────────────────────
{
  const ctx = await withRole('DRIVER', { width: 460, height: 950 })
  const page = await ctx.newPage()
  await page.goto(`${BASE}/deliveries`, { waitUntil: 'networkidle' })
  await page.locator('.font-mono').first().waitFor({ timeout: 15000 }) // nomor order pada kartu
  await settle(page)
  await shot(page, 'driver-02-pool')

  // buka form konfirmasi pada kartu pertama
  await page.getByRole('button', { name: 'Konfirmasi Terkirim' }).first().click()
  await page.getByText('Nama Penerima').waitFor({ timeout: 10000 })
  await page.getByLabel('Nama Penerima').fill('Bapak Surya (Resepsionis)')
  await settle(page)
  await page.getByText('Nama Penerima').scrollIntoViewIfNeeded()
  await shot(page, 'driver-03-form')

  // sorot bagian Foto Bukti (set file contoh agar tampak terisi)
  await page.setInputFiles('input[type="file"]', path.join(IMG, 'logo.png')).catch(() => {})
  await page.getByText('Foto Bukti').scrollIntoViewIfNeeded()
  await settle(page)
  await shot(page, 'driver-04-foto')
  await ctx.close()
}

// ── BAB C — STAF (desktop) ─────────────────────────────────────────────────
const DESK = { width: 1320, height: 860 }
{
  // 01 layout + dashboard operasional (STAFF)
  const ctx = await withRole('STAFF', DESK)
  const page = await ctx.newPage()

  await page.goto(`${BASE}/`, { waitUntil: 'networkidle' })
  await page.getByText('Menu').waitFor({ timeout: 15000 })
  await settle(page, 1200)
  await shot(page, 'staf-01-layout')

  // 03 daftar order
  await page.goto(`${BASE}/orders`, { waitUntil: 'networkidle' })
  await settle(page, 1000)
  await shot(page, 'staf-03-order-list')

  // 04 detail order (ambil id pertama dari API dev)
  const orders = await ctx.request.get(`${API}/api/orders`).then((r) => r.json())
  const oid = (orders.content ?? orders)[0].id
  await page.goto(`${BASE}/orders/${oid}`, { waitUntil: 'networkidle' })
  await page.locator('h1.font-mono').waitFor({ timeout: 15000 })
  await settle(page, 900)
  await shot(page, 'staf-04-order-detail')

  // 05 daftar tagihan
  await page.goto(`${BASE}/billing`, { waitUntil: 'networkidle' })
  await settle(page, 1000)
  await shot(page, 'staf-05-tagihan-list')

  // 06 detail tagihan (navigasi via href link tagihan pertama)
  const link = page.locator('a[href^="/billing/"]').first()
  await link.waitFor({ timeout: 15000 })
  const href = await link.getAttribute('href')
  await page.goto(`${BASE}${href}`, { waitUntil: 'networkidle' })
  await page.getByRole('button', { name: 'Lihat PDF' }).waitFor({ timeout: 15000 })
  await settle(page, 1000)
  await shot(page, 'staf-06-tagihan-detail')

  // 07 laporan
  await page.goto(`${BASE}/reports`, { waitUntil: 'networkidle' })
  await page.getByRole('button', { name: 'Harian' }).waitFor({ timeout: 15000 })
  await settle(page, 1000)
  await shot(page, 'staf-07-laporan')
  await ctx.close()
}
{
  // 02 dashboard analitik (OWNER) — Recharts lazy-loaded
  const ctx = await withRole('OWNER', DESK)
  const page = await ctx.newPage()
  await page.goto(`${BASE}/`, { waitUntil: 'networkidle' })
  await page.locator('svg.recharts-surface, text=Pendapatan').first().waitFor({ timeout: 20000 }).catch(() => {})
  await settle(page, 1800)
  await shot(page, 'staf-02-dasbor')
  await ctx.close()
}

console.log('Bab B + C: screenshot tersimpan di img/')
await browser.close()
