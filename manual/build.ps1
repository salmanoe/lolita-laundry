# Build buku panduan Lolita Laundry -> satu PDF (sampul -> daftar isi -> isi).
# Jalankan: pwsh manual\build.ps1
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $here

$wk = "C:\Program Files\wkhtmltopdf\bin\wkhtmltopdf.exe"
if (-not (Test-Path $wk)) { throw "wkhtmltopdf tidak ditemukan di $wk" }

$out = "Panduan-Lolita-Laundry.pdf"
$css = Get-Content "style.css" -Raw

# ── Sampul (file terpisah; logo di-embed sebagai data URI) ──────────────────
$logoB64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("$here\img\logo-white.png"))
$coverHtml = @"
<!doctype html><html><head><meta charset="utf-8"><style>$css</style></head><body>
<div class="cover"><div class="badge">
<img src="data:image/png;base64,$logoB64" alt="Lolita Laundry" />
<h1>Lolita Laundry</h1>
<p class="subtitle">Buku Panduan Penggunaan Aplikasi</p>
<p class="meta">Untuk Staf Hotel/Klien &bull; Driver &bull; Staf Lolita<br/>Versi 1.0 &bull; Juni 2026</p>
</div></div></body></html>
"@
Set-Content "_cover.html" $coverHtml -Encoding UTF8

# ── Isi (Pandoc -> HTML mandiri; CSS + gambar di-embed) ─────────────────────
pandoc "manual-lolita-laundry.md" -o "_content.html" `
  -s --embed-resources `
  --css=style.css `
  -f markdown+raw_html-implicit_figures `
  --metadata title=" "

# ── Rakit: cover -> toc -> content ──────────────────────────────────────────
& $wk `
  --enable-local-file-access `
  --footer-font-size 8 `
  --footer-font-name "Segoe UI" `
  --footer-center "Halaman [page] dari [topage]" `
  --footer-spacing 4 `
  cover "_cover.html" `
  toc --toc-header-text "Daftar Isi" `
  "_content.html" `
  $out

Remove-Item "_cover.html","_content.html" -ErrorAction SilentlyContinue

if (Test-Path $out) {
  $kb = [math]::Round((Get-Item $out).Length / 1KB, 1)
  Write-Host "OK: $out ($kb KB)" -ForegroundColor Green
} else {
  throw "Build gagal: $out tidak terbentuk"
}