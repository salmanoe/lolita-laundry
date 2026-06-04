import { ReactNode, useEffect } from 'react'
import { useAuth } from './AuthContext'

interface Props {
  children: ReactNode
}

export default function AuthGuard({ children }: Props) {
  const { isLoading, isAuthenticated, loginWithRedirect } = useAuth()

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      loginWithRedirect()
    }
  }, [isLoading, isAuthenticated, loginWithRedirect])

  if (isLoading || !isAuthenticated) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
      </div>
    )
  }

  return <>{children}</>
}
