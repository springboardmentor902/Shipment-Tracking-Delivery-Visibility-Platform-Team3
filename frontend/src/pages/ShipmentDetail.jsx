import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import AppLayout from '../components/AppLayout'
import StatusBadge from '../components/StatusBadge'
import { useAuth } from '../context/AuthContext'
import { extractErrorMessage } from '../services/api'
import { ALLOWED_STATUS_TRANSITIONS, CAN_CHANGE_STATUS_ROLES, shipmentService } from '../services/shipmentService'

function Row({ label, value }) {
  return (
    <div className="flex justify-between gap-6 py-2 text-sm">
      <span className="text-slate-500">{label}</span>
      <span className="text-right font-medium text-slate-900">{value || '—'}</span>
    </div>
  )
}

export default function ShipmentDetail() {
  const { id } = useParams()
  const { user } = useAuth()

  const [shipment, setShipment] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [notice, setNotice] = useState('')
  const [nextStatus, setNextStatus] = useState('')
  const [statusNote, setStatusNote] = useState('')
  const [busy, setBusy] = useState(false)
  const [showCancel, setShowCancel] = useState(false)
  const [cancelReason, setCancelReason] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setShipment(await shipmentService.getById(id))
    } catch (err) {
      const status = err.response?.status
      if (status === 403) setError('You do not have access to this shipment.')
      else if (status === 404) setError('Shipment not found.')
      else setError(extractErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  // CANCELLED is reachable only through the cancel endpoint, so keep it out of the dropdown.
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
    } catch (err) {
      setActionError(extractErrorMessage(err))
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
                    onChange={(e) => setNextStatus(e.target.value)}
                    className="rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none
                               focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
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
                  onClick={handleStatusChange}
                  disabled={busy || !nextStatus}
                  className="rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white
                             hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {busy ? 'Updating…' : 'Update status'}
                </button>
              </>
            )}

            {canCancel && !showCancel && (
              <button
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
                onChange={(e) => setCancelReason(e.target.value)}
                placeholder="Customer requested cancellation"
                className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none
                           focus:border-red-500 focus:ring-2 focus:ring-red-500/20"
              />
              <div className="mt-3 flex gap-2">
                <button
                  onClick={handleCancel}
                  disabled={busy}
                  className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-60"
                >
                  {busy ? 'Cancelling…' : 'Confirm cancellation'}
                </button>
                <button
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

          {!canChangeStatus && !transitionOptions.length && (
            <p className="text-sm text-slate-500">
              This shipment is in a terminal state — no further transitions are possible.
            </p>
          )}
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
    </AppLayout>
  )
}
