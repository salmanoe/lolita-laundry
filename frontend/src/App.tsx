import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import AuthGuard from './auth/AuthGuard'
import RequireSuperAdmin from './auth/RequireSuperAdmin'
import Layout from './components/Layout'
import DriverLayout from './components/DriverLayout'
import DeliveriesPage from './pages/DeliveriesPage'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import ClientsPage from './pages/ClientsPage'

import ClientDetailPage from './pages/ClientDetailPage'
import ItemsPage from './pages/ItemsPage'
import MasterDataPage from './pages/MasterDataPage'
import OrdersPage from './pages/OrdersPage'
import OrderDetailPage from './pages/OrderDetailPage'
import BillingPage from './pages/BillingPage'
import BillingDetailPage from './pages/BillingDetailPage'
import ReportsPage from './pages/ReportsPage'
import UsersPage from './pages/UsersPage'
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
    // Driver delivery app — minimal shell, no admin sidebar. DriverLayout bounces non-drivers to '/'.
    path: '/deliveries',
    element: <AuthGuard><DriverLayout /></AuthGuard>,
    children: [{ index: true, element: <DeliveriesPage /> }],
  },
  {
    // All routes inside here require a valid Auth0 session. Layout redirects DRIVER users to /deliveries.
    element: <AuthGuard><Layout /></AuthGuard>,
    children: [
      { index: true,         element: <DashboardPage /> },
      { path: 'clients',     element: <ClientsPage /> },
      { path: 'clients/:id', element: <ClientDetailPage /> },
      { path: 'items',       element: <RequireSuperAdmin><ItemsPage /></RequireSuperAdmin> },
      { path: 'master-data', element: <RequireSuperAdmin><MasterDataPage /></RequireSuperAdmin> },
      { path: 'orders',      element: <OrdersPage /> },
      { path: 'orders/:id',  element: <OrderDetailPage /> },
      { path: 'billing',     element: <BillingPage /> },
      { path: 'billing/:id', element: <BillingDetailPage /> },
      { path: 'reports',     element: <ReportsPage /> },
      { path: 'users',       element: <RequireSuperAdmin><UsersPage /></RequireSuperAdmin> },
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
