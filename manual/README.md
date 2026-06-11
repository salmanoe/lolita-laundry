# Buku Panduan Lolita Laundry (PDF untuk end-user)

Satu PDF berbahasa Indonesia untuk tiga audiens: **Staf Hotel/Klien**, **Driver**, **Staf Lolita**.

## Hasil

`Panduan-Lolita-Laundry.pdf` — dibangun dari `manual-lolita-laundry.md` + `style.css`.

## Prasyarat (sekali pasang)

- **Pandoc** — `winget install JohnMacFarlane.Pandoc`
- **wkhtmltopdf** — `winget install wkhtmltopdf.wkhtmltox` (mesin PDF; tanpa LaTeX)

## Membangun PDF

```powershell
pwsh manual\build.ps1
```

Mengeksekusi Pandoc → wkhtmltopdf dengan `style.css`, daftar isi, dan nomor halaman.

## Memperbarui screenshot

Screenshot ada di `img/`. Untuk menangkap ulang dari aplikasi yang berjalan:

| Bagian                    | Skrip                | Syarat                                                                        |
|---------------------------|----------------------|-------------------------------------------------------------------------------|
| Halaman login             | `capture/login.mjs`  | frontend `:5173` jalan (mode apa pun)                                         |
| Bab A — form order publik | `capture/public.mjs` | backend `:8080` + frontend `:5173`; token PBS di skrip                        |
| Bab B & C — driver + staf | `capture/app.mjs`    | backend profil **dev** + frontend **mock** (`VITE_AUTH_MOCK=true`) di `:5173` |

`app.mjs` memakai Playwright dan meng-intercept `GET /api/me` untuk memaksa role
(DRIVER / STAFF / OWNER) tanpa mengubah DB. Jalankan:

```powershell
cd manual\capture
node public.mjs ; node login.mjs ; node app.mjs
pwsh ..\build.ps1
```

> Catatan: `public.mjs` membuat satu order asli di DB dev (wajar untuk dev).
> `app.mjs` tidak menyimpan konfirmasi pengiriman (hanya membuka form).