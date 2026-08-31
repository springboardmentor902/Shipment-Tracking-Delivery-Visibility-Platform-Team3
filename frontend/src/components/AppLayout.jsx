import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import EnableAlertsButton from './EnableAlertsButton'
import NotificationBell from './NotificationBell'

export default function AppLayout({ children }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const canMonitor = ['LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR'].includes(user?.role)
  const canViewBusinessAccount = ['BUSINESS_CLIENT', 'ADMINISTRATOR'].includes(user?.role)

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  const links = [
    { to: '/shipments', label: 'Shipments' },
    { to: '/track', label: 'Track' },
    { to: '/delays', label: 'Delays' },
    ...(canMonitor ? [{ to: '/monitoring', label: 'Monitoring' }] : []),
    ...(canViewBusinessAccount ? [{ to: '/business-account', label: 'Business account' }] : []),
    { to: '/profile', label: 'Profile' },
  ]

  function isActive(to) {
    return to === '/shipments'
      ? location.pathname.startsWith('/shipments')
      : location.pathname === to
  }

  function navLinks() {
    return links.map((link) => (
      <Link
        key={link.to}
        to={link.to}
        className={`rounded-lg px-3 py-2 transition ${
          isActive(link.to)
            ? 'bg-brand-50 text-brand-700'
            : 'text-slate-600 hover:bg-slate-50 hover:text-brand-700'
        }`}
      >
        {link.label}
      </Link>
    ))
  }

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-5 px-4 sm:px-6">
          <Link
            to="/shipments"
            className="shrink-0 font-semibold tracking-tight text-slate-900 hover:text-brand-700"
          >
            ShipTrack Pro
          </Link>

          <nav
            className="hidden items-center gap-1 text-sm font-medium md:flex"
            aria-label="Main navigation"
          >
            {navLinks()}
          </nav>

          <div className="ml-auto flex shrink-0 items-center gap-3">
            <span className="hidden text-sm text-slate-500 xl:inline">
              {user?.fullName} · {user?.role?.replaceAll('_', ' ')}
            </span>

            <NotificationBell />

            <EnableAlertsButton />

            <button
              onClick={handleLogout}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Log out
            </button>
          </div>
        </div>

        <nav className="h-11 border-t border-slate-100 md:hidden" aria-label="Main navigation">
          <div className="mx-auto flex h-full max-w-6xl items-center gap-1 overflow-x-auto px-4 text-sm font-medium sm:px-6">
            {navLinks()}
          </div>
        </nav>
      </header>

      <main className="mx-auto max-w-6xl px-6 py-8">{children}</main>
    </div>
  )
}