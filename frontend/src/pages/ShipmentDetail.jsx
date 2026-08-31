import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import AppLayout from '../components/AppLayout'
import LiveStatusPill from '../components/LiveStatusPill'
import RouteLegsPanel from '../components/RouteLegsPanel'
import StatusBadge from '../components/StatusBadge'
import TextField from '../components/TextField'
import TrackingTimeline from '../components/TrackingTimeline'
import { useAuth } from '../context/AuthContext'
import useLiveTracking from '../hooks/useLiveTracking'
import { extractErrorMessage } from '../services/api'
import { ALLOWED_STATUS_TRANSITIONS, CAN_CHANGE_STATUS_ROLES, shipmentService } from '../services/shipmentService'

function Row({ label, value }) {
  return (
    <div className="flex justify-between gap-6 py-2 text-sm">
      <span className="text-slate-500">{label}</span>
      <span className="text-right font-medium text-slate-900">{value ?? '—'}</span>
    </div>
  )
}

export default function ShipmentDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const canManageOperations = ['LOGISTICS_OPERATOR', 'ADMINISTRATOR'].includes(user?.role)

  const [shipment, setShipment] = useState(null)
  const [events, setEvents] = useState([])
  const [operators, setOperators] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [notice, setNotice] = useState('')
  const [nextStatus, setNextStatus] = useState('')
  const [statusNote, setStatusNote] = useState('')
  const [busy, setBusy] = useState(false)
  const [showCancel, setShowCancel] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [operatorId, setOperatorId] = useState('')
  const [locationForm, setLocationForm] = useState({ location: '', latitude: '', longitude: '', notes: '' })
  const [checkpointForm, setCheckpointForm] = useState({ location: '', notes: '' })
  const [livePosition, setLivePosition] = useState(null)

  /**
   * Applies a pushed update straight to the screen: the timeline gains a row,
   * the map marker moves and a status change updates the header, all without a
   * refetch.
   */
  const handleLiveUpdate = useCallback((update) => {
    if (update.latitude != null && update.longitude != null) {
      setLivePosition({
        latitude: update.latitude,
        longitude: update.longitude,
        location: update.location,
        recordedAt: update.recordedAt,
      })
    }

    if (update.status) {
      setShipment((previous) => (previous ? { ...previous, status: update.status } : previous))
    }

    setEvents((previous) => {
      const row = {
        id: `live-${update.recordedAt || Date.now()}-${previous.length}`,
        status: update.status,
        location: update.location,
        latitude: update.latitude,
        longitude: update.longitude,
        notes: update.notes,
        recordedByName: update.recordedByName,
        recordedAt: update.recordedAt,
      }
      // the operator's own POST already appended this event
      const duplicate = previous.some(
        (event) => event.recordedAt === row.recordedAt && event.location === row.location
      )
      return duplicate ? previous : [...previous, row]
    })
  }, [])

  const { status: liveStatus, error: liveError, lastUpdate } = useLiveTracking({
    destination: `/topic/shipments/${id}`,
    enabled: Boolean(id),
    onUpdate: handleLiveUpdate,
  })

  const loadTracking = useCallback(async () => {
    try {
      setEvents(await shipmentService.getTrackingEvents(id))
    } catch (err) {
      if (err.response?.status !== 404) {
        setActionError(extractErrorMessage(err, 'Could not load tracking history.'))
      }
    }
  }, [id])

  const loadOperators = useCallback(async () => {
    if (user?.role !== 'ADMINISTRATOR') return
    try {
      const users = await shipmentService.getAdminUsers()
      setOperators(users.filter((item) => item.role === 'LOGISTICS_OPERATOR'))
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Could not load operators.'))
    }
  }, [user?.role])

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setShipment(await shipmentService.getById(id))
      await Promise.all([loadTracking(), loadOperators()])
    } catch (err) {
      const status = err.response?.status
      if (status === 403) setError('You do not have access to this shipment.')
      else if (status === 404) setError('Shipment not found.')
      else setError(extractErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [id, loadOperators, loadTracking])

  useEffect(() => {
    load()
  }, [load])

  const transitionOptions = (ALLOWED_STATUS_TRANSITIONS[shipment?.status] || []).filter(
    (status) => status !== 'CANCELLED'
  )
  const canChangeStatus = CAN_CHANGE_STATUS_ROLES.includes(user?.role) && transitionOptions.length > 0
  const canEdit =
    shipment &&
    !['CANCELLED', 'DELIVERED'].includes(shipment.status) &&
    ['BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'ADMINISTRATOR'].includes(user?.role) &&
    (user?.role === 'ADMINISTRATOR' || shipment.createdById === user?.id || shipment.assignedOperatorId === user?.id)
  const canCancel =
    shipment &&
    shipment.status !== 'CANCELLED' &&
    shipment.status !== 'DELIVERED' &&
    (user?.role === 'ADMINISTRATOR' || shipment.createdById === user?.id)

  async function handleStatusChange() {
    if (!nextStatus) return
    setBusy(true)
    setActionError('')
    setNotice('')
    try {
      const updated = await shipmentService.updateStatus(id, nextStatus, statusNote.trim())
      setShipment(updated)
      setNextStatus('')
      setStatusNote('')
      setNotice(`Status moved to ${updated.status.replaceAll('_', ' ')}.`)
      await loadTracking()
    } catch (err) {
      setActionError(extractErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function handleCancel() {
    if (!cancelReason.trim()) {
      setActionError('A cancellation reason is required.')
      return
    }
    setBusy(true)
    setActionError('')
    setNotice('')
    try {
      const updated = await shipmentService.cancel(id, cancelReason.trim())
      setShipment(updated)
      setShowCancel(false)
      setCancelReason('')
      setNotice('Shipment cancelled.')
      await loadTracking()
    } catch (err) {
      setActionError(extractErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function handleAssignOperator(event) {
    event.preventDefault()
    if (!operatorId) {
      setActionError('Enter or select an operator id.')
      return
    }
    setBusy(true)
    setActionError('')
    setNotice('')
    try {
      const updated = await shipmentService.assignOperator(id, Number(operatorId))
      setShipment(updated)
      setOperatorId('')
      setNotice('Operator assigned.')
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Could not assign the operator.'))
    } finally {
      setBusy(false)
    }
  }

  function handleCheckpointChange(event) {
    const { name, value } = event.target
    setCheckpointForm((previous) => ({ ...previous, [name]: value }))
    setActionError('')
    setNotice('')
  }

  /** Adds a timeline checkpoint; the server geocodes the place name if it can. */
  async function handleCheckpointSubmit(event) {
    event.preventDefault()
    if (!checkpointForm.location.trim()) {
      setActionError('A checkpoint location is required.')
      return
    }

    setBusy(true)
    setActionError('')
    setNotice('')
    try {
      await shipmentService.addTrackingEvent({
        shipmentId: Number(id),
        location: checkpointForm.location.trim(),
        notes: checkpointForm.notes.trim() || undefined,
      })
      setCheckpointForm({ location: '', notes: '' })
      setNotice('Checkpoint added to the timeline.')
      await loadTracking()
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Could not add the checkpoint.'))
    } finally {
      setBusy(false)
    }
  }

  function handleLocationChange(event) {
    const { name, value } = event.target
    setLocationForm((previous) => ({ ...previous, [name]: value }))
    setActionError('')
    setNotice('')
  }

  async function handleLocationSubmit(event) {
    event.preventDefault()
    const latitude = Number(locationForm.latitude)
    const longitude = Number(locationForm.longitude)
    if (!locationForm.location.trim()) {
      setActionError('Location is required.')
      return
    }
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      setActionError('Enter valid latitude and longitude values.')
      return
    }

    setBusy(true)
    setActionError('')
    setNotice('')
    try {
      await shipmentService.updateLocation({
        shipmentId: Number(id),
        latitude,
        longitude,
        location: locationForm.location.trim(),
        notes: locationForm.notes.trim(),
      })
      setLocationForm({ location: '', latitude: '', longitude: '', notes: '' })
      setNotice('Location update recorded.')
      await Promise.all([loadTracking(), shipmentService.getById(id).then(setShipment)])
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Could not record the location update.'))
    } finally {
      setBusy(false)
    }
  }

  if (loading) {
    return (
      <AppLayout>
        <p className="text-slate-500">Loading…</p>
      </AppLayout>
    )
  }

  if (error) {
    return (
      <AppLayout>
        <div className="rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">{error}</div>
        <Link to="/shipments" className="mt-4 inline-block text-sm text-brand-600">
          ← Back to shipments
        </Link>
      </AppLayout>
    )
  }

  return (
    <AppLayout>
      <div className="mb-6">
        <Link to="/shipments" className="text-sm text-brand-600 hover:text-brand-700">
          ← Back to shipments
        </Link>
        <div className="mt-2 flex flex-wrap items-center gap-3">
          <h1 className="font-mono text-xl font-semibold text-slate-900">{shipment.trackingNumber}</h1>
          <StatusBadge status={shipment.status} />
          <span className="text-sm text-slate-500">{shipment.priority}</span>
          <LiveStatusPill
            status={liveStatus}
            error={liveError}
            updatedAt={lastUpdate?.recordedAt || livePosition?.recordedAt}
          />
          {canEdit && (
            <Link
              to={`/shipments/${id}/edit`}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Edit shipment
            </Link>
          )}
        </div>
      </div>

      {notice && (
        <div className="mb-4 rounded-lg bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-700">{notice}</div>
      )}
      {actionError && (
        <div role="alert" className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
          {actionError}
        </div>
      )}

      {(canChangeStatus || canCancel) && (
        <section className="mb-6 rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">Actions</h2>
          <div className="flex flex-wrap items-end gap-3">
            {canChangeStatus && (
              <>
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-slate-700">Move status to</label>
                  <select
                    value={nextStatus}
                    onChange={(event) => setNextStatus(event.target.value)}
                    className="rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
                  >
                    <option value="">Select…</option>
                    {transitionOptions.map((status) => (
                      <option key={status} value={status}>
                        {status.replaceAll('_', ' ')}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label htmlFor="statusNote" className="mb-1.5 block text-sm font-medium text-slate-700">
                    Update note (optional)
                  </label>
                  <input
                    id="statusNote"
                    value={statusNote}
                    onChange={(event) => setStatusNote(event.target.value)}
                    placeholder="Add a delivery update"
                    className="rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
                  />
                </div>
                <button
                  type="button"
                  onClick={handleStatusChange}
                  disabled={busy || !nextStatus}
                  className="rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {busy ? 'Updating…' : 'Update status'}
                </button>
              </>
            )}

            {canCancel && !showCancel && (
              <button
                type="button"
                onClick={() => setShowCancel(true)}
                className="rounded-lg border border-red-300 px-4 py-2.5 text-sm font-medium text-red-700 hover:bg-red-50"
              >
                Cancel shipment
              </button>
            )}
          </div>

          {showCancel && (
            <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-4">
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Reason for cancelling</label>
              <input
                value={cancelReason}
                onChange={(event) => setCancelReason(event.target.value)}
                placeholder="Customer requested cancellation"
                className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-red-500 focus:ring-2 focus:ring-red-500/20"
              />
              <div className="mt-3 flex gap-2">
                <button
                  type="button"
                  onClick={handleCancel}
                  disabled={busy}
                  className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-60"
                >
                  {busy ? 'Cancelling…' : 'Confirm cancellation'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setShowCancel(false)
                    setCancelReason('')
                  }}
                  className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-white"
                >
                  Keep shipment
                </button>
              </div>
            </div>
          )}
        </section>
      )}

      {canManageOperations && (
        <section className="mb-6 rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-1 text-sm font-semibold uppercase tracking-wide text-slate-500">Operations</h2>
          <p className="mb-4 text-sm text-slate-500">Assign an operator or record the latest location.</p>
          <div className="grid gap-6 lg:grid-cols-2">
            <form onSubmit={handleAssignOperator} className="space-y-3">
              <label htmlFor="operatorId" className="block text-sm font-medium text-slate-700">
                Assign operator
              </label>
              {user?.role === 'ADMINISTRATOR' && operators.length ? (
                <select
                  id="operatorId"
                  value={operatorId}
                  onChange={(event) => setOperatorId(event.target.value)}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
                >
                  <option value="">Select an operator</option>
                  {operators.map((operator) => (
                    <option key={operator.id} value={operator.id}>
                      {operator.fullName} (#{operator.id})
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  id="operatorId"
                  value={operatorId}
                  onChange={(event) => setOperatorId(event.target.value)}
                  type="number"
                  min="1"
                  placeholder="Operator id"
                  className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
                />
              )}
              <button
                type="submit"
                disabled={busy}
                className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {busy ? 'Saving…' : 'Assign operator'}
              </button>
            </form>

            <form onSubmit={handleLocationSubmit} className="grid gap-3 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <TextField id="location" label="Current location" value={locationForm.location} onChange={handleLocationChange} />
              </div>
              <TextField id="latitude" label="Latitude" type="number" value={locationForm.latitude} onChange={handleLocationChange} />
              <TextField id="longitude" label="Longitude" type="number" value={locationForm.longitude} onChange={handleLocationChange} />
              <div className="sm:col-span-2">
                <TextField id="notes" label="Location note (optional)" value={locationForm.notes} onChange={handleLocationChange} />
              </div>
              <div className="sm:col-span-2">
                <button
                  type="submit"
                  disabled={busy}
                  className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {busy ? 'Saving…' : 'Save location update'}
                </button>
              </div>
            </form>
          </div>
        </section>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Sender</h2>
          <div className="divide-y divide-slate-100">
            <Row label="Name" value={shipment.senderName} />
            <Row label="Phone" value={shipment.senderPhone} />
            <Row label="Address" value={shipment.senderAddress} />
            <Row label="Pickup" value={shipment.pickupAddress} />
          </div>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Receiver</h2>
          <div className="divide-y divide-slate-100">
            <Row label="Name" value={shipment.receiverName} />
            <Row label="Phone" value={shipment.receiverPhone} />
            <Row label="Email" value={shipment.receiverEmail} />
            <Row label="Address" value={shipment.receiverAddress} />
            <Row label="Delivery" value={shipment.deliveryAddress} />
          </div>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Tracking</h2>
          <div className="divide-y divide-slate-100">
            <Row label="Created by" value={`${shipment.createdByName} (${shipment.createdByRole})`} />
            <Row label="Assigned operator" value={shipment.assignedOperatorName} />
            <Row label="Estimated delivery" value={shipment.estimatedDeliveryDate} />
            <Row label="Actual delivery" value={shipment.actualDeliveryDate} />
            <Row label="Created at" value={shipment.createdAt?.replace('T', ' ').slice(0, 16)} />
            <Row label="Updated at" value={shipment.updatedAt?.replace('T', ' ').slice(0, 16)} />
            {shipment.cancelledAt && (
              <>
                <Row label="Cancelled at" value={shipment.cancelledAt.replace('T', ' ').slice(0, 16)} />
                <Row label="Reason" value={shipment.cancellationReason} />
              </>
            )}
          </div>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">
            Packages ({shipment.totalPackages})
          </h2>
          <div className="space-y-3">
            {shipment.packages?.map((item) => (
              <div key={item.id} className="rounded-lg border border-slate-200 p-3 text-sm">
                <div className="flex items-center justify-between">
                  <span className="font-medium text-slate-900">
                    #{item.packageNo} · {item.description}
                  </span>
                  {item.fragile && (
                    <span className="rounded-full bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-700">
                      Fragile
                    </span>
                  )}
                </div>
                <p className="mt-1 text-slate-500">
                  {item.weightKg} kg · qty {item.quantity}
                  {item.lengthCm && ` · ${item.lengthCm}×${item.widthCm}×${item.heightCm} cm`}
                  {item.declaredValue && ` · ₹${item.declaredValue}`}
                </p>
              </div>
            ))}
          </div>
        </section>
      </div>

      <RouteLegsPanel shipmentId={id} canManage={canManageOperations} livePosition={livePosition} />

      <section className="mt-6 rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">Tracking timeline</h2>
        <TrackingTimeline events={events} />

        {canManageOperations && (
          <form onSubmit={handleCheckpointSubmit} className="mt-5 grid gap-3 border-t border-slate-100 pt-5 sm:grid-cols-2">
            <TextField
              id="checkpointLocation"
              name="location"
              label="Checkpoint location"
              value={checkpointForm.location}
              onChange={handleCheckpointChange}
              placeholder="Vijayawada hub"
            />
            <TextField
              id="checkpointNotes"
              name="notes"
              label="Checkpoint note (optional)"
              value={checkpointForm.notes}
              onChange={handleCheckpointChange}
            />
            <div className="sm:col-span-2">
              <button
                type="submit"
                disabled={busy}
                className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {busy ? 'Saving…' : 'Add checkpoint'}
              </button>
            </div>
          </form>
        )}
      </section>
    </AppLayout>
  )
}
