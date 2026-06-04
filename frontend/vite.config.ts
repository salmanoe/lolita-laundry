import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// When running inside Docker, VITE_BACKEND_PROXY_URL is set to http://backend:8080
// so the Vite proxy forwards /api/* to the backend container by name.
// Locally (without Docker) it falls back to http://localhost:8080.
const backendUrl = process.env.VITE_BACKEND_PROXY_URL ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,        // listen on 0.0.0.0 so the container port is reachable from the host
    port: 5173,
    strictPort: true,  // fail loudly if 5173 is taken instead of silently moving to 5174
                       // (a moved port breaks the Auth0 callback URL allow-list)
    proxy: {
      '/api':    { target: backendUrl, changeOrigin: true },
      '/public': { target: backendUrl, changeOrigin: true },
    },
  },
})
