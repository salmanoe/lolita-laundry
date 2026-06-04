import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { AuthProvider } from './auth/AuthContext'
import App from './App'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,   // 5 min
      retry: 1,
    },
  },
})

// StrictMode is deliberately INSIDE AuthProvider: in dev it double-mounts its
// children, and a double-mounted Auth0Provider exchanges the single-use auth code
// twice → the second exchange fails and bounces login into an infinite loop.
// Keeping Auth0Provider above StrictMode makes its redirect handling run once.
createRoot(document.getElementById('root')!).render(
  <AuthProvider>
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <App />
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </StrictMode>
  </AuthProvider>,
)
