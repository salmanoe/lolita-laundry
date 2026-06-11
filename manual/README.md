# Buku Panduan Lolita Laundry (untuk end-user)

Satu dokumen berbahasa Indonesia untuk tiga audiens: **Staf Hotel/Klien**, **Driver**, **Staf Lolita**.
Tersedia dua format: **PDF** (cetak, brand penuh) dan **DOCX** (Word, mudah diedit).

## Hasil

- `Panduan-Lolita-Laundry.pdf` — dari `manual-lolita-laundry.md` + `style.css`.
- `Panduan-Lolita-Laundry.docx` — dari naskah yang sama + reference doc berbrand.

## Prasyarat (sekali pasang)

- **Pandoc** — `winget install JohnMacFarlane.Pandoc`
- **wkhtmltopdf** — `winget install wkhtmltopdf.wkhtmltox` (mesin PDF; tanpa LaTeX)
- **Python + python-docx** — `python -m pip install python-docx` (hanya untuk DOCX berbrand)

## Membangun PDF

```powershell
pwsh manual\build.ps1
```

Pandoc → wkhtmltopdf (`cover` → `toc` → isi) dengan `style.css`, daftar isi, dan nomor halaman.

## Membangun DOCX

```powershell
pwsh manual\build-docx.ps1
```

`make_reference.py` membuat `reference.docx` berbrand (heading navy/ocean, logo di header), lalu
Pandoc merakit DOCX dengan lebar gambar disetel agar pas kolom Word.

> Daftar Isi DOCX adalah *field* Word — bila kosong saat dibuka, klik di dalamnya lalu tekan **F9**.

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