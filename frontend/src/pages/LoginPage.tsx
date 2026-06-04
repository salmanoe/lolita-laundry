import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function LoginPage() {
  const { isAuthenticated, isLoading, loginWithRedirect } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (isAuthenticated) navigate('/', { replace: true })
  }, [isAuthenticated, navigate])

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
      </div>
    )
  }

  return (
    <div className="flex h-screen flex-col items-center justify-center bg-gray-50">
      <div className="rounded-xl bg-white p-10 shadow-md text-center w-80">
        <h1 className="mb-2 text-2xl font-bold text-gray-800">Lolita Laundry</h1>
        <p className="mb-6 text-sm text-gray-500">Masuk untuk melanjutkan</p>
        <button
          onClick={() => loginWithRedirect()}
          className="w-full rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-500"
        >
          Masuk
        </button>
      </div>
    </div>
  )
}
