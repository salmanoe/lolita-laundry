// Tangkap halaman login (publik, konteks bersih → belum terautentikasi).
import { chromium } from 'playwright'
import { fileURLToPath } from 'url'
import path from 'path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const IMG = path.resolve(__dirname, '..', 'img')
const BASE = 'http://localhost:5173'

const browser = await chromium.launch()
const ctx = await browser.newContext({ viewport: { width: 460, height: 760 }, deviceScaleFactor: 2 })
const page = await ctx.newPage()
await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' })
await page.getByRole('button', { name: 'Masuk' }).waitFor({ timeout: 15000 })
await page.waitForTimeout(400)
await page.screenshot({ path: path.join(IMG, 'driver-01-login.png') })
console.log('Login: 1 screenshot tersimpan.')
await browser.close()