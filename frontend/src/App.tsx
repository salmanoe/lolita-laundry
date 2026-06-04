import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import AuthGuard from './auth/AuthGuard'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import ClientsPage from './pages/ClientsPage'

import ClientDetailPage from './pages/ClientDetailPage'
import ItemsPage from './pages/ItemsPage'
import MasterDataPage from './pages/MasterDataPage'
import OrdersPage from './pages/OrdersPage'
import OrderDetailPage from './pages/OrderDetailPage'
import PublicOrderPage from './pages/PublicOrderPage'
import NotFoundPage from './pages/NotFoundPage'

const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    // Public tokenized order form — no Auth0 session required
    path: '/order/:token',
    element: <PublicOrderPage />,
  },
  {
    // All routes inside here require a valid Auth0 session
    element: <AuthGuard><Layout /></AuthGuard>,
    children: [
      { index: true,         element: <DashboardPage /> },
      { path: 'clients',     element: <ClientsPage /> },
      { path: 'clients/:id', element: <ClientDetailPage /> },
      { path: 'items',       element: <ItemsPage /> },
      { path: 'master-data', element: <MasterDataPage /> },
      { path: 'orders',      element: <OrdersPage /> },
      { path: 'orders/:id',  element: <OrderDetailPage /> },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
])

export default function App() {
  return <RouterProvider router={router} />
}
