import { useState } from 'react'
import { Link } from 'react-router-dom'
import StatusBadge from '../components/StatusBadge'
import { extractErrorMessage } from '../services/api'
import { SHIPMENT_STATUSES, shipmentService } from '../services/shipmentService'

function formatDate(value) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

export default function Tracking() {
  const [trackingNumber, setTrackingNumber] = useState('')
  const [shipment, setShipment] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event) {
    event.preventDefault()
    const value = trackingNumber.trim()
    if (!value) {
      setError('Enter a tracking number.')
      return
    }

    setLoading(true)
    setError('')
    setShipment(null)
    try {
      setShipment(await shipmentService.getByTracking(value))
    } catch (err) {
      if (err.response?.status === 404) setError('No shipment was found for that tracking number.')
      else setError(extractErrorMessage(err, 'Could not retrieve tracking information.'))
    } finally {
      setLoading(false)
    }
  }

  const currentIndex = shipment ? SHIPMENT_STATUSES.indexOf(shipment.status) : -1

  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-12">
      <div className="w-full max-w-2xl">
        <div className="mb-8 text-center">
          <Link to="/login" className="text-sm font-medium text-brand-600 hover:text-brand-700">
            ShipTrack Pro
          </Link>
          <h1 className="mt-2 text-2xl font-semibold tracking-tight text-slate-900">Track a shipment</h1>
          <p className="mt-1 text-sm text-slate-500">Enter your tracking number for the latest delivery status.</p>
        </div>

        <form onSubmit={handleSubmit} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:flex sm:gap-3">
          <label htmlFor="trackingNumber" className="sr-only">Tracking number</label>
          <input
            id="trackingNumber"
            value={trackingNumber}
            onChange={(event) => setTrackingNumber(event.target.value)}
            placeholder="STP…"
            className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 font-mono text-sm uppercase outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
          />
          <button
            type="submit"
            disabled={loading}
            className="mt-3 w-full rounded-lg bg-brand-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60 sm:mt-0 sm:w-auto"
          >
            {loading ? 'Tracking…' : 'Track shipment'}
          </button>
        </form>

        {error && (
          <div role="alert" className="mt-5 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
            {error}
          </div>
        )}

        {shipment && (
          <section className="mt-5 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Tracking number</p>
                <h2 className="mt-1 font-mono text-xl font-semibold text-slate-900">{shipment.trackingNumber}</h2>
              </div>
              <StatusBadge status={shipment.status} />
            </div>

            <dl className="mt-6 grid gap-4 border-y border-slate-100 py-5 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-slate-500">Receiver</dt>
                <dd className="mt-1 font-medium text-slate-900">{shipment.receiverName || '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Estimated delivery</dt>
                <dd className="mt-1 font-medium text-slate-900">{shipment.estimatedDeliveryDate || '—'}</dd>
              </div>
              {shipment.actualDeliveryDate && (
                <div>
                  <dt className="text-slate-500">Delivered</dt>
                  <dd className="mt-1 font-medium text-slate-900">{shipment.actualDeliveryDate}</dd>
                </div>
              )}
              <div>
                <dt className="text-slate-500">Last updated</dt>
                <dd className="mt-1 font-medium text-slate-900">{formatDate(shipment.updatedAt)}</dd>
              </div>
            </dl>

            <ol className="mt-6 space-y-3" aria-label="Shipment status timeline">
              {SHIPMENT_STATUSES.filter((status) => status !== 'CANCELLED').map((status) => {
                const index = SHIPMENT_STATUSES.indexOf(status)
                const isCurrent = status === shipment.status
                const isComplete = currentIndex >= index && shipment.status !== 'FAILED_DELIVERY'
                return (
                  <li key={status} className="flex items-center gap-3 text-sm">
                    <span
                      aria-hidden="true"
                      className={`h-2.5 w-2.5 rounded-full ${
                        isCurrent || isComplete ? 'bg-brand-600' : 'bg-slate-200'
                      }`}
                    />
                    <span className={isCurrent ? 'font-semibold text-slate-900' : 'text-slate-500'}>
                      {status.replaceAll('_', ' ')}
                    </span>
                  </li>
                )
              })}
              {shipment.status === 'FAILED_DELIVERY' && (
                <li className="flex items-center gap-3 text-sm font-semibold text-red-700">
                  <span aria-hidden="true" className="h-2.5 w-2.5 rounded-full bg-red-600" />
                  Delivery attempt failed
                </li>
              )}
              {shipment.status === 'CANCELLED' && (
                <li className="flex items-center gap-3 text-sm font-semibold text-slate-700">
                  <span aria-hidden="true" className="h-2.5 w-2.5 rounded-full bg-slate-500" />
                  Shipment cancelled
                </li>
              )}
            </ol>
          </section>
        )}
      </div>
    </main>
  )
}
