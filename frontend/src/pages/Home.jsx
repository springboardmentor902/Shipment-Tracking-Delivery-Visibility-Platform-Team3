import { useRef, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import PublicHeader from '../components/PublicHeader'
import { useAuth } from '../context/AuthContext'

function ArrowIcon() {
  return (
    <svg aria-hidden="true" className="h-4 w-4" viewBox="0 0 16 16" fill="none">
      <path d="M2.5 8h10m-4-4 4 4-4 4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function TrackingIcon() {
  return (
    <svg aria-hidden="true" className="h-6 w-6 text-brand-600" viewBox="0 0 24 24" fill="none">
      <path d="M4 19V5m0 14h16M8 15.5l3-3 2.5 1.5L20 7.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="8" cy="15.5" r="1.25" fill="currentColor" />
      <circle cx="11" cy="12.5" r="1.25" fill="currentColor" />
      <circle cx="13.5" cy="14" r="1.25" fill="currentColor" />
      <circle cx="20" cy="7.5" r="1.25" fill="currentColor" />
    </svg>
  )
}

function PeopleIcon() {
  return (
    <svg aria-hidden="true" className="h-6 w-6 text-brand-600" viewBox="0 0 24 24" fill="none">
      <circle cx="9" cy="8" r="3" stroke="currentColor" strokeWidth="1.8" />
      <path d="M3.5 19c.7-3.1 2.6-4.7 5.5-4.7s4.8 1.6 5.5 4.7M16.5 5.5a2.7 2.7 0 0 1 0 5.1m1.4 3.8c1.5.8 2.3 2.3 2.6 4.6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

function PinIcon() {
  return (
    <svg aria-hidden="true" className="h-6 w-6 text-brand-600" viewBox="0 0 24 24" fill="none">
      <path d="M19 10c0 5-7 10-7 10S5 15 5 10a7 7 0 1 1 14 0Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
      <circle cx="12" cy="10" r="2.3" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  )
}

function ParcelIcon() {
  return (
    <svg aria-hidden="true" className="h-6 w-6 text-brand-600" viewBox="0 0 24 24" fill="none">
      <path d="m4 7 8-4 8 4v10l-8 4-8-4V7Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
      <path d="m4 7 8 4 8-4M12 11v10M9 4.5l8 4" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
    </svg>
  )
}


function Feature({ icon, title, children, className = '' }) {
  return (
    <article className={`rounded-xl border border-slate-200 bg-white p-6 shadow-sm ${className}`}>
      {icon}
      <h3 className="mt-5 text-lg font-semibold tracking-tight text-slate-900">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-slate-600">{children}</p>
    </article>
  )
}

export default function Home() {
  const { isAuthenticated, initialising } = useAuth()
  const navigate = useNavigate()
  const trackingInput = useRef(null)
  const [trackingNumber, setTrackingNumber] = useState('')

  function focusTracker() {
    document.getElementById('home-tracker')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    requestAnimationFrame(() => trackingInput.current?.focus())
  }

  function handleTracking(event) {
    event.preventDefault()
    const number = trackingNumber.trim()
    navigate(number ? `/track?number=${encodeURIComponent(number)}` : '/track')
  }

  if (initialising) return <div className="min-h-screen bg-slate-100" />
  if (isAuthenticated) return <Navigate to="/shipments" replace />

  return (
    <div className="min-h-screen bg-slate-100">
      <PublicHeader />
      <main id="main-content">
        <section className="mx-auto grid max-w-6xl items-center gap-10 px-4 py-14 sm:px-6 md:grid-cols-[1fr_1.05fr] md:py-20">
          <div>
            <p className="text-sm font-semibold text-brand-700">Shipment visibility, without the guesswork</p>
            <h1 className="mt-4 max-w-xl text-4xl font-semibold tracking-tight text-slate-900 sm:text-5xl">
              ShipTrack Pro
            </h1>
            <p className="mt-5 max-w-xl text-lg leading-8 text-slate-600">
              Real time shipment tracking and delivery visibility for the people sending, moving and receiving every parcel.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={focusTracker}
                className="inline-flex min-h-11 items-center gap-2 rounded-lg bg-brand-600 px-5 py-3 text-sm font-medium text-white shadow-sm hover:bg-brand-700"
              >
                Track a shipment
                <ArrowIcon />
              </button>
              <Link to="/login" className="rounded-lg px-3 py-3 text-sm font-medium text-slate-700 hover:text-brand-700">
                Log in
              </Link>
              <Link to="/register" className="rounded-lg px-3 py-3 text-sm font-medium text-brand-700 hover:bg-brand-50">
                Create account
              </Link>
            </div>
          </div>
          <div>
            <img src='src/images/image.png'/>
          </div>
        </section>

        <section id="home-tracker" className="border-y border-slate-200 bg-white scroll-mt-20">
          <div className="mx-auto grid max-w-6xl gap-6 px-4 py-10 sm:px-6 md:grid-cols-[.85fr_1.15fr] md:items-center">
            <div>
              <p className="text-sm font-semibold text-brand-700">Public tracking</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-900">Check a delivery in a few seconds.</h2>
              <p className="mt-3 text-sm leading-6 text-slate-600">Use the tracking number from your shipment to see its current status and update history.</p>
            </div>
            <form onSubmit={handleTracking} className="rounded-xl border border-slate-200 bg-slate-50 p-4 shadow-sm sm:flex sm:gap-3">
              <label htmlFor="home-tracking-number" className="sr-only">Tracking number</label>
              <input
                ref={trackingInput}
                id="home-tracking-number"
                value={trackingNumber}
                onChange={(event) => setTrackingNumber(event.target.value)}
                placeholder="Enter tracking number"
                className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-3 font-mono text-sm uppercase outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
                data-testid="input-home-tracking-number"
              />
              <button
                type="submit"
                className="mt-3 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-lg bg-brand-600 px-5 py-3 text-sm font-medium text-white hover:bg-brand-700 sm:mt-0 sm:w-auto"
                data-testid="button-home-track"
              >
                Track
                <ArrowIcon />
              </button>
            </form>
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 md:py-20">
          <div className="max-w-2xl">
            <p className="text-sm font-semibold text-brand-700">Built around the shipment journey</p>
            <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-900">Useful details at every handoff.</h2>
          </div>
          <div className="mt-8 grid gap-5 md:grid-cols-2">
            <Feature icon={<TrackingIcon />} title="A live timeline for every shipment">
              Each tracking update records a status, location, note, person who updated it and time, so customers can follow the shipment history.
            </Feature>
            <Feature icon={<PeopleIcon />} title="Access that matches the work">
              Customers, business clients, logistics operators, support agents and administrators use role based access for the parts of ShipTrack Pro they need.
            </Feature>
            <Feature icon={<PinIcon />} title="Monitoring for delivery operators">
              Operators can review active deliveries with the last known location and add the next update as a shipment moves.
            </Feature>
            <Feature icon={<ParcelIcon />} title="Lifecycle and package details">
              Create shipments, record package details, move them through delivery stages and cancel a shipment when it cannot continue.
            </Feature>
          </div>
        </section>

        <section className="border-y border-slate-200 bg-white">
          <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6">
            <p className="text-sm font-semibold text-brand-700">How it works</p>
            <div className="mt-6 grid gap-5 sm:grid-cols-4">
              {['Create shipment', 'Pick up', 'In transit', 'Delivered'].map((step, index) => (
                <div key={step} className="border-t-2 border-brand-600 pt-3">
                  <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">0{index + 1}</p>
                  <h3 className="mt-1 text-base font-semibold text-slate-900">{step}</h3>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>
      <footer className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-8 text-sm sm:flex-row sm:items-center sm:justify-between sm:px-6">
        <div>
          <p className="font-semibold text-slate-900">ShipTrack Pro</p>
          <p className="mt-1 text-slate-500">Built for the internship project.</p>
        </div>
        <nav className="flex gap-5 font-medium" aria-label="Footer navigation">
          <Link to="/track" className="text-slate-600 hover:text-brand-700">Track</Link>
          <Link to="/login" className="text-slate-600 hover:text-brand-700">Log in</Link>
        </nav>
      </footer>
    </div>
  )
}
