import { useCallback, useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import StatusBadge from '../components/StatusBadge'
import TrackingTimeline from '../components/TrackingTimeline'
import PublicHeader from '../components/PublicHeader'
import { extractErrorMessage } from '../services/api'
import { trackingService } from '../services/trackingService'

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ').slice(0, 16)
  return date.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })
}

function CheckIcon() {
  return (
    <svg aria-hidden="true" className="h-3.5 w-3.5" viewBox="0 0 16 16" fill="none">
      <path d="m3 8.2 3.1 3.1L13 4.7" stroke="currentColor" strokeWidth="2.1" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function ProgressStrip({ shipment, events }) {
  const stages = [
    { status: 'CREATED', label: 'Created' },
    { status: 'PICKED_UP', label: 'Picked up' },
    { status: 'IN_TRANSIT', label: 'In transit' },
    { status: 'OUT_FOR_DELIVERY', label: 'Out for delivery' },
    ...(shipment.status === 'FAILED_DELIVERY'
      ? [{ status: 'FAILED_DELIVERY', label: 'Failed delivery' }]
      : shipment.status === 'CANCELLED'
        ? [{ status: 'CANCELLED', label: 'Cancelled' }]
        : [{ status: 'DELIVERED', label: 'Delivered' }]),
  ]
  const normalOrder = ['CREATED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED']
  const currentIndex = normalOrder.indexOf(shipment.status)
  const reachedStatuses = new Set([shipment.status, ...(events || []).map((event) => event.status)])

  return (
    <section className="mt-6 border-y border-slate-100 py-5" aria-labelledby="delivery-progress">
      <h3 id="delivery-progress" className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">
        Delivery progress
      </h3>
      <div>
        <ol className="grid grid-cols-3 gap-x-2 gap-y-4 sm:grid-cols-5" aria-label="Shipment lifecycle">
          {stages.map((stage, index) => {
            const reached =
              reachedStatuses.has(stage.status) ||
              (currentIndex >= 0 && index <= currentIndex && stage.status !== 'DELIVERED')
            const current = shipment.status === stage.status

            return (
              <li key={stage.status} className="min-w-0" aria-current={current ? 'step' : undefined}>
                <div className={`mb-2 h-1.5 rounded-full ${reached ? 'bg-brand-600' : 'bg-slate-200'}`} />
                <div className={`flex items-start gap-1 text-xs font-medium ${reached ? 'text-brand-700' : 'text-slate-500'}`}>
                  <span
                    className={`mt-0.5 inline-flex h-3.5 w-3.5 shrink-0 items-center justify-center rounded-full ${
                      reached ? 'bg-brand-600 text-white' : 'border border-slate-300 bg-white text-transparent'
                    }`}
                  >
                    <CheckIcon />
                  </span>
                  <span className="leading-4">{stage.label}</span>
                </div>
              </li>
            )
          })}
        </ol>
      </div>
    </section>
  )
}

function TrackingTable({ events }) {
  if (!events.length) {
    return (
      <div className="hidden rounded-xl border border-dashed border-slate-300 bg-slate-50 px-5 py-8 text-center sm:block">
        <p className="text-sm font-medium text-slate-700">No events recorded yet</p>
        <p className="mt-1 text-sm text-slate-500">Updates will appear here as the shipment moves.</p>
      </div>
    )
  }

  const newestFirst = [...events].sort(
    (first, second) => new Date(second.recordedAt).getTime() - new Date(first.recordedAt).getTime()
  )

  return (
    <div className="hidden overflow-x-auto rounded-xl border border-slate-200 sm:block">
      <table className="w-full min-w-[720px] text-left text-sm">
        <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
          <tr>
            <th scope="col" className="px-4 py-3 font-semibold">Status</th>
            <th scope="col" className="px-4 py-3 font-semibold">Location</th>
            <th scope="col" className="px-4 py-3 font-semibold">Notes</th>
            <th scope="col" className="px-4 py-3 font-semibold">Updated by</th>
            <th scope="col" className="px-4 py-3 font-semibold">Date and time</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 bg-white">
          {newestFirst.map((event) => (
            <tr key={event.id}>
              <td className="px-4 py-3"><StatusBadge status={event.status} /></td>
              <td className="px-4 py-3 font-medium text-slate-800">{event.location || '—'}</td>
              <td className="max-w-xs px-4 py-3 text-slate-600">{event.notes || '—'}</td>
              <td className="px-4 py-3 text-slate-600">{event.recordedByName || 'System'}</td>
              <td className="whitespace-nowrap px-4 py-3 text-slate-600">
                <time dateTime={event.recordedAt}>{formatDate(event.recordedAt)}</time>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default function Tracking() {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryNumber = searchParams.get('number')?.trim() || ''
  const [trackingNumber, setTrackingNumber] = useState(queryNumber)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const lookedUpQuery = useRef('')

  const lookupTracking = useCallback(async (value) => {
    setLoading(true)
    setError('')
    setResult(null)
    try {
      setResult(await trackingService.lookup(value))
    } catch (err) {
      if (err.response?.status === 404) setError('No shipment was found for that tracking number.')
      else setError(extractErrorMessage(err, 'Could not retrieve tracking information.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!queryNumber || lookedUpQuery.current === queryNumber) return
    lookedUpQuery.current = queryNumber
    setTrackingNumber(queryNumber)
    lookupTracking(queryNumber)
  }, [lookupTracking, queryNumber])

  async function handleSubmit(event) {
    event.preventDefault()
    const value = trackingNumber.trim()
    if (!value) {
      setError('Enter a tracking number.')
      return
    }

    lookedUpQuery.current = value
    setSearchParams({ number: value })
    await lookupTracking(value)
  }

  const shipment = result?.shipment

  return (
    <div className="min-h-screen">
      <PublicHeader />
      <main className="mx-auto w-full max-w-5xl px-4 py-10 sm:px-6 sm:py-12">
        <div className="mx-auto max-w-2xl text-center">
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Track a shipment</h1>
          <p className="mt-1 text-sm text-slate-500">Enter your tracking number for the latest delivery status.</p>
        </div>

        <form onSubmit={handleSubmit} className="mx-auto mt-7 max-w-2xl rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:flex sm:gap-3">
          <label htmlFor="trackingNumber" className="sr-only">Tracking number</label>
          <input
            id="trackingNumber"
            value={trackingNumber}
            onChange={(event) => setTrackingNumber(event.target.value)}
            placeholder="STP…"
            className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 font-mono text-sm uppercase outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
            data-testid="input-tracking-number"
          />
          <button
            type="submit"
            disabled={loading}
            className="mt-3 w-full rounded-lg bg-brand-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60 sm:mt-0 sm:w-auto"
            data-testid="button-track-shipment"
          >
            {loading ? 'Tracking…' : 'Track shipment'}
          </button>
        </form>

        {error && (
          <div role="alert" className="mx-auto mt-5 max-w-2xl rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
            {error}
          </div>
        )}

        {shipment && (
          <section className="mt-8 rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6" data-testid="tracking-result">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Tracking number</p>
                <h2 className="mt-1 font-mono text-xl font-semibold text-slate-900">{shipment.trackingNumber}</h2>
              </div>
              <StatusBadge status={shipment.status} />
            </div>

            <dl className="mt-6 grid gap-5 border-y border-slate-100 py-5 text-sm sm:grid-cols-2 lg:grid-cols-3">
              <div>
                <dt className="text-slate-500">Sender</dt>
                <dd className="mt-1 font-medium text-slate-900">{shipment.senderName || '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Receiver</dt>
                <dd className="mt-1 font-medium text-slate-900">{shipment.receiverName || '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Priority</dt>
                <dd className="mt-1 font-medium text-slate-900">{shipment.priority || '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Packages</dt>
                <dd className="mt-1 font-medium text-slate-900">{shipment.totalPackages ?? shipment.packages?.length ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Pickup address</dt>
                <dd className="mt-1 font-medium text-slate-900">{shipment.pickupAddress || shipment.senderAddress || '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Delivery address</dt>
                <dd className="mt-1 font-medium text-slate-900">{shipment.deliveryAddress || shipment.receiverAddress || '—'}</dd>
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

            <ProgressStrip shipment={shipment} events={result?.events} />

            <div className="mt-6 border-t border-slate-100 pt-5">
              <h3 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">
                Tracking history
              </h3>
              <TrackingTable events={result?.events || []} />
              <div className="sm:hidden">
                <TrackingTimeline events={result?.events} />
              </div>
            </div>
          </section>
        )}
      </main>
    </div>
  )
}
