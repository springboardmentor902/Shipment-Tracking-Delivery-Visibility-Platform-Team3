import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import AppLayout from '../components/AppLayout'
import StatusBadge from '../components/StatusBadge'
import { useAuth } from '../context/AuthContext'
import { extractErrorMessage } from '../services/api'
import { CAN_CREATE_ROLES, SHIPMENT_STATUSES, shipmentService } from '../services/shipmentService'

export default function ShipmentList() {
  const { user } = useAuth()
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState('')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const canCreate = CAN_CREATE_ROLES.includes(user?.role)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setData(await shipmentService.list({ status: statusFilter, page, size: 10 }))
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not load shipments.'))
    } finally {
      setLoading(false)
    }
  }, [statusFilter, page])

  useEffect(() => {
    load()
  }, [load])

  const shipments = data?.content || []

  return (
    <AppLayout>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Shipments</h1>
          <p className="mt-1 text-sm text-slate-500">
            {canCreate
              ? 'Book new shipments and track their progress.'
              : 'Shipments linked to your account.'}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <select
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value)
              setPage(0)
            }}
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none
                       focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
          >
            <option value="">All statuses</option>
            {SHIPMENT_STATUSES.map((status) => (
              <option key={status} value={status}>
                {status.replaceAll('_', ' ')}
              </option>
            ))}
          </select>

          {canCreate && (
            <Link
              to="/shipments/new"
              className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
            >
              New shipment
            </Link>
          )}
        </div>
      </div>

      {error && (
        <div role="alert" className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
        <table className="min-w-full divide-y divide-slate-200 text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th className="px-4 py-3 font-medium">Tracking</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Receiver</th>
              <th className="px-4 py-3 font-medium">Priority</th>
              <th className="px-4 py-3 font-medium">Packages</th>
              <th className="px-4 py-3 font-medium">ETA</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && (
              <tr>
                <td colSpan={7} className="px-4 py-10 text-center text-slate-500">
                  Loading…
                </td>
              </tr>
            )}

            {!loading && shipments.length === 0 && (
              <tr>
                <td colSpan={7} className="px-4 py-10 text-center text-slate-500">
                  No shipments yet.{' '}
                  {canCreate && (
                    <Link to="/shipments/new" className="font-medium text-brand-600">
                      Create the first one
                    </Link>
                  )}
                </td>
              </tr>
            )}

            {!loading &&
              shipments.map((shipment) => (
                <tr key={shipment.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 font-mono text-xs text-slate-900">
                    {shipment.trackingNumber}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={shipment.status} />
                  </td>
                  <td className="px-4 py-3 text-slate-700">{shipment.receiverName}</td>
                  <td className="px-4 py-3 text-slate-500">{shipment.priority}</td>
                  <td className="px-4 py-3 text-slate-500">{shipment.totalPackages}</td>
                  <td className="px-4 py-3 text-slate-500">
                    {shipment.actualDeliveryDate || shipment.estimatedDeliveryDate || '—'}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Link
                      to={`/shipments/${shipment.id}`}
                      className="font-medium text-brand-600 hover:text-brand-700"
                    >
                      View
                    </Link>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between text-sm text-slate-500">
          <span>
            Page {data.number + 1} of {data.totalPages} · {data.totalElements} total
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={data.number === 0}
              className="rounded-lg border border-slate-300 px-3 py-1.5 font-medium text-slate-700
                         hover:bg-white disabled:opacity-50"
            >
              Previous
            </button>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={data.number + 1 >= data.totalPages}
              className="rounded-lg border border-slate-300 px-3 py-1.5 font-medium text-slate-700
                         hover:bg-white disabled:opacity-50"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </AppLayout>
  )
}
