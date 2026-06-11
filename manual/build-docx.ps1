# Build buku panduan Lolita Laundry -> Word (.docx).
# Jalankan: pwsh manual\build-docx.ps1
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $here

$out = "Panduan-Lolita-Laundry.docx"
$md = Get-Content "manual-lolita-laundry.md" -Raw

# Word tidak punya CSS: beri lebar gambar eksplisit agar pas di kolom.
# Screenshot staf = landscape (lebar penuh); klien/driver = potret ponsel (sempit).
$md = [regex]::Replace($md, '\]\(img/(staf-[^)]+\.png)\)',   '](img/$1){width=6.3in}')
$md = [regex]::Replace($md, '\]\(img/((klien|driver)-[^)]+\.png)\)', '](img/$1){width=3.3in}')

# Sisipkan blok judul (halaman judul Word) di atas.
$front = @"
---
title: "Panduan Penggunaan Lolita Laundry"
subtitle: "Buku Panduan Aplikasi — Staf Hotel/Klien · Driver · Staf Lolita"
date: "Versi 1.0 — Juni 2026"
lang: id
---

"@
$tmp = "_docx.md"
Set-Content $tmp ($front + $md) -Encoding UTF8

# Reference doc berbrand (heading navy/ocean, logo di header).
# Selalu mulai dari default Pandoc agar branding tidak menumpuk (mis. logo dobel).
cmd /c "pandoc --print-default-data-file reference.docx > reference.docx"
python make_reference.py

pandoc $tmp -o $out `
  -f markdown+raw_html-implicit_figures `
  --toc --toc-depth=2 `
  --resource-path=. `
  --reference-doc="reference.docx" `
  --metadata title="Panduan Penggunaan Lolita Laundry" `
  --metadata toc-title="Daftar Isi"

Remove-Item $tmp -ErrorAction SilentlyContinue

if (Test-Path $out) {
  $kb = [math]::Round((Get-Item $out).Length / 1KB, 1)
  Write-Host "OK: $out ($kb KB)" -ForegroundColor Green
} else {
  throw "Build gagal: $out tidak terbentuk"
}
