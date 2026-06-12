import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import AuthGuard from './auth/AuthGuard'
import RequireSuperAdmin from './auth/RequireSuperAdmin'
import RoleLayout from './components/RoleLayout'
import DeliveriesPage from './pages/DeliveriesPage'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import ClientsPage from './pages/ClientsPage'

import ClientDetailPage from './pages/ClientDetailPage'
import ItemsPage from './pages/ItemsPage'
import MasterDataPage from './pages/MasterDataPage'
import OrdersPage from './pages/OrdersPage'
import NewOrderPage from './pages/NewOrderPage'
import OrderDetailPage from './pages/OrderDetailPage'
import BillingPage from './pages/BillingPage'
import BillingDetailPage from './pages/BillingDetailPage'
import ReportsPage from './pages/ReportsPage'
import UsersPage from './pages/UsersPage'
import NotFoundPage from './pages/NotFoundPage'

const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    // All routes require a valid Auth0 session. RoleLayout picks the chrome by role:
    // DAILY_STAFF get the minimal operator shell (Buat Order / Order / Pengantaran); everyone
    // else gets the admin sidebar.
    element: <AuthGuard><RoleLayout /></AuthGuard>,
    children: [
      { index: true,         element: <DashboardPage /> },
      { path: 'clients',     element: <ClientsPage /> },
      { path: 'clients/:id', element: <ClientDetailPage /> },
      { path: 'items',       element: <RequireSuperAdmin><ItemsPage /></RequireSuperAdmin> },
      { path: 'master-data', element: <RequireSuperAdmin><MasterDataPage /></RequireSuperAdmin> },
      { path: 'orders',      element: <OrdersPage /> },
      { path: 'orders/new',  element: <NewOrderPage /> },
      { path: 'orders/:id',  element: <OrderDetailPage /> },
      { path: 'billing',     element: <BillingPage /> },
      { path: 'billing/:id', element: <BillingDetailPage /> },
      { path: 'reports',     element: <ReportsPage /> },
      { path: 'users',       element: <RequireSuperAdmin><UsersPage /></RequireSuperAdmin> },
      { path: 'deliveries',  element: <DeliveriesPage /> },
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
