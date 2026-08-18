import { Navigate, Route, Routes } from 'react-router-dom'
import Login from './pages/Login'
import Register from './pages/Register'
import ShipmentList from './pages/ShipmentList'
import ShipmentCreate from './pages/ShipmentCreate'
import ShipmentDetail from './pages/ShipmentDetail'
import ShipmentEdit from './pages/ShipmentEdit'
import Tracking from './pages/Tracking'
import NotFound from './pages/NotFound'
import ProtectedRoute from './routes/ProtectedRoute'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/shipments" replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/track" element={<Tracking />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<Navigate to="/shipments" replace />} />
        <Route path="/shipments" element={<ShipmentList />} />
        <Route path="/shipments/:id" element={<ShipmentDetail />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['BUSINESS_CLIENT', 'LOGISTICS_OPERATOR']} />}>
        <Route path="/shipments/new" element={<ShipmentCreate />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'ADMINISTRATOR']} />}>
        <Route path="/shipments/:id/edit" element={<ShipmentEdit />} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
