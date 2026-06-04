import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="flex h-screen flex-col items-center justify-center gap-4 text-gray-500">
      <p className="text-5xl font-bold text-gray-200">404</p>
      <p className="text-sm">Halaman tidak ditemukan.</p>
      <Link to="/" className="text-sm text-brand-600 hover:underline">
        Kembali ke Dasbor
      </Link>
    </div>
  )
}
