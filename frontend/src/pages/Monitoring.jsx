import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import AppLayout from '../components/AppLayout'
import LiveStatusPill from '../components/LiveStatusPill'
import StatusBadge from '../components/StatusBadge'
import { useAuth } from '../context/AuthContext'
import useLiveTracking from '../hooks/useLiveTracking'
import { extractErrorMessage } from '../services/api'
import { shipmentService } from '../services/shipmentService'

const EMPTY_LOCATION = {
  shipmentId: '',
  latitude: '',
  longitude: '',
  location: '',
  notes: '',
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ').slice(0, 16)
  return date.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })
}

export default function Monitoring() {
  const { user } = useAuth()
  const [shipments, setShipments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [lastRefresh, setLastRefresh] = useState(null)
  const [now, setNow] = useState(Date.now())
  const [locationForm, setLocationForm] = useState(EMPTY_LOCATION)
  const [locationError, setLocationError] = useState('')
  const [updatingLocation, setUpdatingLocation] = useState(false)
  const canUpdateLocation = ['LOGISTICS_OPERATOR', 'ADMINISTRATOR'].includes(user?.role)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setShipments(await shipmentService.getActiveMonitoring())
      setLastRefresh(Date.now())
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not load active deliveries.'))
    } finally {
      setLoading(false)
    }
  }, [])

  /**
   * Patches the row of the shipment that just moved. A push for a shipment that
   * is not on screen yet (a delivery that just went active) triggers a reload.
   */
  const handleLiveUpdate = useCallback(
    (update) => {
      let known = false
      setShipments((previous) =>
        previous.map((row) => {
          if (row.shipmentId !== update.shipmentId) return row
          known = true
          return {
            ...row,
            status: update.status || row.status,
            lastLocation: update.location || row.lastLocation,
            lastLatitude: update.latitude ?? row.lastLatitude,
            lastLongitude: update.longitude ?? row.lastLongitude,
            lastUpdatedAt: update.recordedAt || row.lastUpdatedAt,
          }
        })
      )
      setLastRefresh(Date.now())
      if (!known) load()
    },
    [load]
  )

  const { status: liveStatus, error: liveError, lastUpdate } = useLiveTracking({
    destination: '/topic/monitoring/active',
    onUpdate: handleLiveUpdate,
  })

  useEffect(() => {
    load()
    // the socket carries the changes; the poll is only a safety net now
    const refreshTimer = window.setInterval(load, 60000)
    const clockTimer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => {
      window.clearInterval(refreshTimer)
      window.clearInterval(clockTimer)
    }
  }, [load])

  function updatedAgo() {
    if (!lastRefresh) return 'not updated yet'
    const seconds = Math.max(0, Math.floor((now - lastRefresh) / 1000))
    return `updated ${seconds}s ago`
  }

  function handleLocationChange(event) {
    const { name, value } = event.target
    setLocationForm((previous) => ({ ...previous, [name]: value }))
    setLocationError('')
    setNotice('')
  }

  async function handleLocationSubmit(event) {
    event.preventDefault()
    const shipmentId = Number(locationForm.shipmentId)
    const latitude = Number(locationForm.latitude)
    const longitude = Number(locationForm.longitude)
    if (!shipmentId || !locationForm.location.trim()) {
      setLocationError('Shipment id and location are required.')
      return
    }
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      setLocationError('Enter valid latitude and longitude values.')
      return
    }

    setUpdatingLocation(true)
    setLocationError('')
    setNotice('')
    try {
      await shipmentService.updateLocation({
        shipmentId,
        latitude,
        longitude,
        location: locationForm.location.trim(),
        notes: locationForm.notes.trim(),
      })
      setLocationForm(EMPTY_LOCATION)
      setNotice('Location update recorded.')
      await load()
    } catch (err) {
      setLocationError(extractErrorMessage(err, 'Could not record the location update.'))
    } finally {
      setUpdatingLocation(false)
    }
  }

  return (
    <AppLayout>
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-xl font-semibold text-slate-900">Live monitoring</h1>
            <LiveStatusPill status={liveStatus} error={liveError} updatedAt={lastUpdate?.recordedAt} />
          </div>
          <p className="mt-1 text-sm text-slate-500">
            {liveStatus === 'live'
              ? 'Driver positions arrive the moment they are recorded.'
              : 'Falling back to a refresh every 60 seconds.'}{' '}
            {updatedAgo()}.
          </p>
        </div>
        <button
          type="button"
          onClick={load}
          disabled={loading}
          className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-white disabled:cursor-not-allowed disabled:opacity-60"
        >
          {loading ? 'Refreshing…' : 'Refresh now'}
        </button>
      </div>

      {notice && (
        <div className="mb-4 rounded-lg bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-700">{notice}</div>
      )}
      {error && (
        <div role="alert" className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
          {error}
        </div>
      )}

      {canUpdateLocation && (
        <section className="mb-6 rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-1 text-sm font-semibold uppercase tracking-wide text-slate-500">Location update</h2>
          <p className="mb-4 text-sm text-slate-500">Record the latest location for an active shipment.</p>
          {locationError && (
            <div role="alert" className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
              {locationError}
            </div>
          )}
          <form onSubmit={handleLocationSubmit} className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
            <div>
              <label htmlFor="shipmentId" className="mb-1.5 block text-sm font-medium text-slate-700">
                Shipment id
              </label>
              <input
                id="shipmentId"
                name="shipmentId"
                type="number"
                min="1"
                value={locationForm.shipmentId}
                onChange={handleLocationChange}
                className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
              />
            </div>
            <div>
              <label htmlFor="location" className="mb-1.5 block text-sm font-medium text-slate-700">
                Location
              </label>
              <input
                id="location"
                name="location"
                value={locationForm.location}
                onChange={handleLocationChange}
                placeholder="Hyderabad hub"
                className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
              />
            </div>
            <div>
              <label htmlFor="latitude" className="mb-1.5 block text-sm font-medium text-slate-700">
                Latitude
              </label>
              <input
                id="latitude"
                name="latitude"
                type="number"
                step="any"
                value={locationForm.latitude}
                onChange={handleLocationChange}
                placeholder="17.4401"
                className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
              />
            </div>
            <div>
              <label htmlFor="longitude" className="mb-1.5 block text-sm font-medium text-slate-700">
                Longitude
              </label>
              <input
                id="longitude"
                name="longitude"
                type="number"
                step="any"
                value={locationForm.longitude}
                onChange={handleLocationChange}
                placeholder="78.3489"
                className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
              />
            </div>
            <div>
              <label htmlFor="notes" className="mb-1.5 block text-sm font-medium text-slate-700">
                Notes
              </label>
              <input
                id="notes"
                name="notes"
                value={locationForm.notes}
                onChange={handleLocationChange}
                placeholder="Optional update"
                className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
              />
            </div>
            <div className="md:col-span-2 lg:col-span-5">
              <button
                type="submit"
                disabled={updatingLocation}
                className="rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {updatingLocation ? 'Saving…' : 'Save location update'}
              </button>
            </div>
          </form>
        </section>
      )}

      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
        <table className="min-w-full divide-y divide-slate-200 text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th className="px-4 py-3 font-medium">Tracking</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Receiver</th>
              <th className="px-4 py-3 font-medium">Last location</th>
              <th className="px-4 py-3 font-medium">Coordinates</th>
              <th className="px-4 py-3 font-medium">Updated</th>
              <th className="px-4 py-3 font-medium">ETA</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && (
              <tr>
                <td colSpan={7} className="px-4 py-10 text-center text-slate-500">
                  Loading active deliveries…
                </td>
              </tr>
            )}
            {!loading && shipments.length === 0 && (
              <tr>
                <td colSpan={7} className="px-4 py-10 text-center text-slate-500">
                  No active deliveries right now.
                </td>
              </tr>
            )}
            {!loading &&
              shipments.map((shipment) => (
                <tr key={shipment.shipmentId} className={shipment.delayed ? 'bg-red-50' : 'hover:bg-slate-50'}>
                  <td className="px-4 py-3 font-mono text-xs text-slate-900">
                    <Link to={`/shipments/${shipment.shipmentId}`} className="hover:text-brand-700">
                      {shipment.trackingNumber}
                    </Link>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <StatusBadge status={shipment.status} />
                      {shipment.delayed && (
                        <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700">
                          Delayed
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-slate-700">
                    <p>{shipment.receiverName}</p>
                    <p className="mt-1 text-xs text-slate-500">{shipment.assignedOperatorName || 'Unassigned'}</p>
                  </td>
                  <td className="px-4 py-3 text-slate-700">{shipment.lastLocation || '—'}</td>
                  <td className="px-4 py-3 font-mono text-xs text-slate-500">
                    {shipment.lastLatitude ?? '—'}, {shipment.lastLongitude ?? '—'}
                  </td>
                  <td className="px-4 py-3 text-slate-500">{formatDateTime(shipment.lastUpdatedAt)}</td>
                  <td className="px-4 py-3 text-slate-500">{shipment.estimatedDeliveryDate || '—'}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
    </AppLayout>
  )
}
