import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function AppLayout({ children }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const canMonitor = ['LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR'].includes(user?.role)
  const canViewBusinessAccount = ['BUSINESS_CLIENT', 'ADMINISTRATOR'].includes(user?.role)

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-6 py-4">
          <div className="flex flex-wrap items-center gap-5">
            <Link to="/shipments" className="font-semibold text-slate-900">
              ShipTrack Pro
            </Link>
            <nav className="flex flex-wrap items-center gap-3 text-sm font-medium text-slate-600" aria-label="Main navigation">
              <Link to="/shipments" className="hover:text-brand-700">
                Shipments
              </Link>
              <Link to="/track" className="hover:text-brand-700">
                Track
              </Link>
              {canMonitor && (
                <Link to="/monitoring" className="hover:text-brand-700">
                  Monitoring
                </Link>
              )}
              {canViewBusinessAccount && (
                <Link to="/business-account" className="hover:text-brand-700">
                  Business account
                </Link>
              )}
              <Link to="/profile" className="hover:text-brand-700">
                Profile
              </Link>
            </nav>
          </div>
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
      <main className="mx-auto max-w-6xl px-6 py-8">{children}</main>
    </div>
  )
}
