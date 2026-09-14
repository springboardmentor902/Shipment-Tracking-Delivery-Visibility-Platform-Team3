import { Link } from 'react-router-dom'

function LogoMark() {
  return (
    <svg aria-hidden="true" className="h-7 w-7 shrink-0 text-brand-600" viewBox="0 0 32 32" fill="none">
      <path d="M4.5 10.5 16 4l11.5 6.5v11L16 28 4.5 21.5v-11Z" stroke="currentColor" strokeWidth="2.4" strokeLinejoin="round" />
      <path d="m4.5 10.5 11.5 6.7 11.5-6.7M16 17.2V28" stroke="currentColor" strokeWidth="2.4" strokeLinejoin="round" />
    </svg>
  )
}

export default function PublicHeader() {
  return (
    <header className="sticky top-0 z-40 h-16 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="mx-auto flex h-full max-w-6xl items-center px-4 sm:px-6">
        <Link
          to="/"
          className="flex shrink-0 items-center gap-2 font-semibold tracking-tight text-slate-900 hover:text-brand-700"
          aria-label="ShipTrack Pro home"
        >
          <LogoMark />
          <span>ShipTrack Pro</span>
        </Link>
        <nav className="ml-4 flex items-center gap-1 text-sm font-medium whitespace-nowrap sm:ml-auto" aria-label="Public navigation">
          <Link to="/track" className="rounded-lg px-2.5 py-2 text-slate-600 hover:bg-slate-50 hover:text-brand-700">
            Track
          </Link>
          <Link to="/login" className="rounded-lg px-2.5 py-2 text-slate-600 hover:bg-slate-50 hover:text-brand-700">
            Log in
          </Link>
          <Link to="/register" className="rounded-lg bg-brand-600 px-3 py-2 text-white hover:bg-brand-700">
            Sign up
          </Link>
        </nav>
      </div>
    </header>
  )
}
