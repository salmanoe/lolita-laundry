// Tangkap screenshot Bab A — form order publik (tanpa login).
// Jalankan: node public.mjs
import { chromium } from 'playwright'
import { fileURLToPath } from 'url'
import path from 'path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const IMG = path.resolve(__dirname, '..', 'img')
const TOKEN = '8b3e3aa5-9231-4e37-b6be-7249c2348b29' // Pasar Baru Square (PER_DEPARTMENT: dept + treatment)
const BASE = 'http://localhost:5173'

const shot = (page, name) => page.screenshot({ path: path.join(IMG, name + '.png') })

const browser = await chromium.launch()
const ctx = await browser.newContext({ viewport: { width: 460, height: 950 }, deviceScaleFactor: 2 })
const page = await ctx.newPage()

await page.goto(`${BASE}/order/${TOKEN}`, { waitUntil: 'networkidle' })
await page.getByText('Formulir Order Laundry').waitFor()
await page.waitForTimeout(400)

// 01 — form awal (header hotel + Nama Staff + Treatment)
await shot(page, 'klien-01-form')

// isi Nama Staff (wajib agar tombol Kirim aktif)
await page.getByPlaceholder('Nama Anda').fill('Dewi Kartika')

// 02 — isi beberapa qty pada daftar item
const inputs = page.locator('input[type="number"]')
const n = await inputs.count()
await inputs.nth(0).fill('10')
if (n > 2) await inputs.nth(2).fill('4')
if (n > 4) await inputs.nth(4).fill('6')
// scroll ke bagian Item lalu screenshot
await page.getByRole('heading', { name: 'Item' }).scrollIntoViewIfNeeded()
await page.waitForTimeout(300)
await shot(page, 'klien-02-isi')

// 03 — centang Treatment, lalu screenshot bagian atas
await page.getByText('Treatment', { exact: false }).first().scrollIntoViewIfNeeded()
const cb = page.locator('input[type="checkbox"]').first()
await cb.check()
await page.waitForTimeout(300)
await page.evaluate(() => window.scrollTo(0, 0))
await page.waitForTimeout(200)
await shot(page, 'klien-03-treatment')

// 04 — kirim order → layar sukses
await cb.uncheck() // kirim sebagai Reguler agar tidak menggandakan harga di data dev
await page.getByRole('button', { name: 'Kirim Order' }).click()
await page.getByText('Order terkirim!').waitFor({ timeout: 10000 })
await page.waitForTimeout(400)
await shot(page, 'klien-04-sukses')

console.log('Bab A: 4 screenshot tersimpan di img/')
await browser.close()