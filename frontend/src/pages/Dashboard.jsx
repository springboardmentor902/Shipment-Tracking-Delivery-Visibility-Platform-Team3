import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const CAN_CREATE = ['BUSINESS_CLIENT', 'LOGISTICS_OPERATOR']

export default function Dashboard() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <span className="font-semibold text-slate-900">ShipTrack Pro</span>
          <div className="flex items-center gap-4">
            <span className="text-sm text-slate-500">
              {user?.fullName} · {user?.role?.replaceAll('_', ' ')}
            </span>
            <button
              onClick={handleLogout}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Log out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-10">
        <h1 className="text-xl font-semibold text-slate-900">Welcome back, {user?.fullName}</h1>
        <p className="mt-1 text-sm text-slate-500">
          {CAN_CREATE.includes(user?.role)
            ? 'You can create and manage shipments.'
            : 'You can view and track shipments assigned to you.'}
        </p>

        <div className="mt-8 rounded-xl border border-dashed border-slate-300 bg-white p-10 text-center text-sm text-slate-500">
          Shipment list and booking form land here in the next ticket.
        </div>
      </main>
    </div>
  )
}
