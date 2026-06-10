import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import UploadProcess from './pages/UploadProcess'
import ValidateOcr from './pages/ValidateOcr'
import InvoiceHistory from './pages/InvoiceHistory'
import Profile from './pages/Profile'

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route path="/" element={
        <ProtectedRoute>
          <Layout>
            <Navigate to="/dashboard" replace />
          </Layout>
        </ProtectedRoute>
      } />

      {[
        { path: '/dashboard', element: <Dashboard /> },
        { path: '/upload', element: <UploadProcess /> },
        { path: '/validate/:id', element: <ValidateOcr /> },
        { path: '/history', element: <InvoiceHistory /> },
        { path: '/profile', element: <Profile /> },
      ].map(({ path, element }) => (
        <Route key={path} path={path} element={
          <ProtectedRoute>
            <Layout>{element}</Layout>
          </ProtectedRoute>
        } />
      ))}

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}
