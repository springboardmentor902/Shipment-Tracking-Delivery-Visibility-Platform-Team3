import { useCallback, useEffect, useState } from 'react'
import RouteMap from './RouteMap'
import TextField from './TextField'
import { extractErrorMessage } from '../services/api'
import {
  METRICS_SOURCE_LABELS,
  ROUTE_LEG_STATUSES,
  formatDuration,
  routeService,
} from '../services/routeService'

const EMPTY_LEG = {
  originAddress: '',
  destinationAddress: '',
  waypoints: '',
  driverId: '',
  distanceKm: '',
  expectedDurationMinutes: '',
  notes: '',
}

const TRAFFIC_STYLES = {
  LIGHT: 'bg-emerald-50 text-emerald-700',
  MODERATE: 'bg-amber-50 text-amber-700',
  HEAVY: 'bg-orange-50 text-orange-700',
  SEVERE: 'bg-red-50 text-red-700',
}

function LegStatusBadge({ status }) {
  const styles = {
    PLANNED: 'bg-slate-100 text-slate-700',
    ACTIVE: 'bg-blue-50 text-blue-700',
    COMPLETED: 'bg-emerald-50 text-emerald-700',
    SKIPPED: 'bg-slate-100 text-slate-500',
  }
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${styles[status] || styles.PLANNED}`}>
      {status || 'PLANNED'}
    </span>
  )
}

/**
 * Multi-leg route planner and viewer.
 *
 * Read-only for customers and business clients; operators and admins can add
 * legs, change a leg's status and pull fresh distance/traffic from Google Maps.
 */
export default function RouteLegsPanel({ shipmentId, canManage = false }) {
  const [legs, setLegs] = useState([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY_LEG)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setLegs(await routeService.listByShipment(shipmentId))
      setError('')
    } catch (err) {
      if (err.response?.status === 404) setLegs([])
      else setError(extractErrorMessage(err, 'Could not load the route.'))
    } finally {
      setLoading(false)
    }
  }, [shipmentId])

  useEffect(() => {
    load()
  }, [load])

  function handleChange(event) {
    const { name, value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
    setError('')
    setNotice('')
  }

  async function handleCreate(event) {
    event.preventDefault()
    if (!form.originAddress.trim() || !form.destinationAddress.trim()) {
      setError('Origin and destination addresses are required.')
      return
    }

    setBusy(true)
    setError('')
    setNotice('')
    try {
      // Distance and duration are optional: the server fills them from Google
      // Maps, or estimates them when Maps is unavailable.
      await routeService.create({
        shipmentId: Number(shipmentId),
        originAddress: form.originAddress.trim(),
        destinationAddress: form.destinationAddress.trim(),
        waypoints: form.waypoints.trim() || undefined,
        driverId: form.driverId ? Number(form.driverId) : undefined,
        distanceKm: form.distanceKm ? Number(form.distanceKm) : undefined,
        expectedDurationMinutes: form.expectedDurationMinutes
          ? Number(form.expectedDurationMinutes)
          : undefined,
        notes: form.notes.trim() || undefined,
      })
      setForm(EMPTY_LEG)
      setShowForm(false)
      setNotice('Route leg added.')
      await load()
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not add the route leg.'))
    } finally {
      setBusy(false)
    }
  }

  async function handleRefresh(legId) {
    setBusy(true)
    setError('')
    setNotice('')
    try {
      await routeService.refreshFromMaps(legId)
      setNotice('Distance, duration and traffic refreshed.')
      await load()
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not refresh this leg.'))
    } finally {
      setBusy(false)
    }
  }

  async function handleStatusChange(legId, status) {
    setBusy(true)
    setError('')
    setNotice('')
    try {
      await routeService.update(legId, { status })
      setNotice(`Leg moved to ${status.toLowerCase()}.`)
      await load()
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not update the leg status.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="mt-6 rounded-xl border border-slate-200 bg-white p-6">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
            Route ({legs.length} {legs.length === 1 ? 'leg' : 'legs'})
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            Distance, duration and traffic come from Google Maps when a server key is configured.
          </p>
        </div>
        {canManage && (
          <button
            type="button"
            onClick={() => setShowForm((previous) => !previous)}
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            {showForm ? 'Close' : 'Add leg'}
          </button>
        )}
      </div>

      {notice && (
        <div className="mb-4 rounded-lg bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-700">{notice}</div>
      )}
      {error && (
        <div role="alert" className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
          {error}
        </div>
      )}

      {loading ? (
        <p className="text-sm text-slate-500">Loading route…</p>
      ) : (
        <>
          <RouteMap legs={legs} />

          {legs.length === 0 ? (
            <p className="mt-4 text-sm text-slate-500">No route legs have been planned yet.</p>
          ) : (
            <ol className="mt-4 space-y-3">
              {legs.map((leg) => (
                <li key={leg.id} className="rounded-lg border border-slate-200 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-full bg-brand-50 px-2 py-0.5 text-xs font-semibold text-brand-700">
                        Leg {leg.legNumber}
                      </span>
                      <LegStatusBadge status={leg.status} />
                      {leg.trafficCondition && (
                        <span
                          className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                            TRAFFIC_STYLES[leg.trafficCondition] || 'bg-slate-100 text-slate-600'
                          }`}
                        >
                          {leg.trafficCondition.toLowerCase()} traffic
                        </span>
                      )}
                    </div>
                    {canManage && (
                      <div className="flex flex-wrap items-center gap-2">
                        <select
                          value={leg.status || 'PLANNED'}
                          onChange={(event) => handleStatusChange(leg.id, event.target.value)}
                          disabled={busy}
                          aria-label={`Status for leg ${leg.legNumber}`}
                          className="rounded-lg border border-slate-300 bg-white px-2.5 py-1.5 text-xs outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
                        >
                          {ROUTE_LEG_STATUSES.map((status) => (
                            <option key={status} value={status}>
                              {status.toLowerCase()}
                            </option>
                          ))}
                        </select>
                        <button
                          type="button"
                          onClick={() => handleRefresh(leg.id)}
                          disabled={busy}
                          className="rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
                        >
                          Refresh from Maps
                        </button>
                      </div>
                    )}
                  </div>

                  <p className="mt-2 text-sm font-medium text-slate-900">
                    {leg.originAddress} → {leg.destinationAddress}
                  </p>
                  {leg.waypoints && <p className="text-xs text-slate-500">via {leg.waypoints}</p>}

                  <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-4">
                    <div>
                      <dt className="text-slate-500">Distance</dt>
                      <dd className="font-medium text-slate-900">
                        {leg.distanceKm ? `${leg.distanceKm} km` : '—'}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">Duration</dt>
                      <dd className="font-medium text-slate-900">
                        {formatDuration(leg.expectedDurationMinutes)}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">With traffic</dt>
                      <dd className="font-medium text-slate-900">
                        {formatDuration(leg.durationInTrafficMinutes)}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">Driver</dt>
                      <dd className="font-medium text-slate-900">{leg.driverName || 'Unassigned'}</dd>
                    </div>
                  </dl>

                  <p className="mt-2 text-xs text-slate-500">
                    {METRICS_SOURCE_LABELS[leg.metricsSource] || 'Source unknown'}
                    {leg.lastLocationAt &&
                      ` · last position ${new Date(leg.lastLocationAt).toLocaleString([], {
                        dateStyle: 'medium',
                        timeStyle: 'short',
                      })}`}
                  </p>
                  {leg.notes && <p className="mt-1 text-sm text-slate-600">{leg.notes}</p>}
                </li>
              ))}
            </ol>
          )}
        </>
      )}

      {canManage && showForm && (
        <form onSubmit={handleCreate} className="mt-5 grid gap-4 border-t border-slate-100 pt-5 sm:grid-cols-2">
          <TextField id="originAddress" label="Origin address" value={form.originAddress} onChange={handleChange} />
          <TextField
            id="destinationAddress"
            label="Destination address"
            value={form.destinationAddress}
            onChange={handleChange}
          />
          <TextField
            id="waypoints"
            label="Waypoints (optional)"
            value={form.waypoints}
            onChange={handleChange}
            placeholder="Kurnool;Anantapur"
          />
          <TextField
            id="driverId"
            label="Driver id (optional)"
            type="number"
            value={form.driverId}
            onChange={handleChange}
          />
          <TextField
            id="distanceKm"
            label="Distance km (optional, Maps fills this)"
            type="number"
            value={form.distanceKm}
            onChange={handleChange}
          />
          <TextField
            id="expectedDurationMinutes"
            label="Duration minutes (optional)"
            type="number"
            value={form.expectedDurationMinutes}
            onChange={handleChange}
          />
          <div className="sm:col-span-2">
            <TextField id="notes" label="Notes (optional)" value={form.notes} onChange={handleChange} />
          </div>
          <div className="sm:col-span-2">
            <button
              type="submit"
              disabled={busy}
              className="rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {busy ? 'Saving…' : 'Add route leg'}
            </button>
          </div>
        </form>
      )}
    </section>
  )
}
