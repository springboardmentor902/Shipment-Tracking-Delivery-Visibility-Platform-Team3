import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import AppLayout from '../components/AppLayout'
import StatusBadge from '../components/StatusBadge'
import { useAuth } from '../context/AuthContext'
import { extractErrorMessage } from '../services/api'
import { shipmentService } from '../services/shipmentService'

export default function ShipmentList() {
  const { user } = useAuth()
  const [statusFilter, setStatusFilter] = useState('')
  const [shipments, setShipments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const canCreate = ['CUSTOMER', 'BUSINESS_CLIENT'].includes(user?.role)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')

    try {
      const data = await shipmentService.list()

      const shipmentList = Array.isArray(data) ? data : []

      const filtered = statusFilter
        ? shipmentList.filter((shipment) => shipment.status === statusFilter)
        : shipmentList

      setShipments(filtered)
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not load shipments.'))
      setShipments([])
    } finally {
      setLoading(false)
    }
  }, [statusFilter])

  useEffect(() => {
    load()
  }, [load])

  const statuses = [
    'CREATED',
    'PICKED_UP',
    'IN_TRANSIT',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
    'FAILED_DELIVERY',
    'CANCELLED',
  ]

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
            onChange={(event) => setStatusFilter(event.target.value)}
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none"
          >
            <option value="">All statuses</option>

            {statuses.map((status) => (
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
        <div
          role="alert"
          className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700"
        >
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
              <th className="px-4 py-3 font-medium">Package</th>
              <th className="px-4 py-3 font-medium">Weight</th>
              <th className="px-4 py-3 font-medium">Created</th>
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
                  No shipments found.
                  {canCreate && (
                    <>
                      {' '}
                      <Link
                        to="/shipments/new"
                        className="font-medium text-brand-600"
                      >
                        Create one
                      </Link>
                    </>
                  )}
                </td>
              </tr>
            )}

            {!loading &&
              shipments.map((shipment) => (
                <tr
                  key={shipment.id}
                  className="hover:bg-slate-50"
                >
                  <td className="px-4 py-3 font-mono text-xs text-slate-900">
                    {shipment.trackingNumber}
                  </td>

                  <td className="px-4 py-3">
                    <StatusBadge status={shipment.status} />
                  </td>

                  <td className="px-4 py-3 text-slate-700">
                    {shipment.receiverName || '—'}
                  </td>

                  <td className="px-4 py-3 text-slate-500">
                    {shipment.packageDescription || '—'}
                  </td>

                  <td className="px-4 py-3 text-slate-500">
                    {shipment.weightKg != null
                      ? `${shipment.weightKg} kg`
                      : '—'}
                  </td>

                  <td className="px-4 py-3 text-slate-500">
                    {shipment.createdAt
                      ? new Date(shipment.createdAt).toLocaleDateString()
                      : '—'}
                  </td>

                  <td className="px-4 py-3 text-right">
                    <Link
                      to={`/shipments/${encodeURIComponent(
                        shipment.trackingNumber
                      )}`}
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
    </AppLayout>
  )
}