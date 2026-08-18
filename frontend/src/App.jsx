import { Navigate, Route, Routes } from 'react-router-dom'
import Login from './pages/Login'
import Register from './pages/Register'
import ShipmentList from './pages/ShipmentList'
import ShipmentCreate from './pages/ShipmentCreate'
import ShipmentDetail from './pages/ShipmentDetail'
import ShipmentEdit from './pages/ShipmentEdit'
import Tracking from './pages/Tracking'
import Monitoring from './pages/Monitoring'
import Profile from './pages/Profile'
import ForgotPassword from './pages/ForgotPassword'
import ResetPassword from './pages/ResetPassword'
import BusinessAccount from './pages/BusinessAccount'
import NotFound from './pages/NotFound'
import ProtectedRoute from './routes/ProtectedRoute'
import Home from './pages/Home'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/track" element={<Tracking />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<Navigate to="/shipments" replace />} />
        <Route path="/shipments" element={<ShipmentList />} />
        <Route path="/shipments/:id" element={<ShipmentDetail />} />
        <Route path="/profile" element={<Profile />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['BUSINESS_CLIENT', 'LOGISTICS_OPERATOR']} />}>
        <Route path="/shipments/new" element={<ShipmentCreate />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'ADMINISTRATOR']} />}>
        <Route path="/shipments/:id/edit" element={<ShipmentEdit />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR']} />}>
        <Route path="/monitoring" element={<Monitoring />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['BUSINESS_CLIENT', 'ADMINISTRATOR']} />}>
        <Route path="/business-account" element={<BusinessAccount />} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
