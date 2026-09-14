import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import AppLayout from '../components/AppLayout'
import {
  StatCard,
  SectionCard,
  StatusBarChart,
  LoadingState,
} from '../components/AnalyticsComponents'
import { useAuth } from '../context/AuthContext'
import api, { extractErrorMessage } from '../services/api'

export default function CustomerDashboard() {
  const { isAuthenticated, initialising } = useAuth()

  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (initialising) return

    if (!isAuthenticated) {
      setLoading(false)
      return
    }

    async function loadAnalytics() {
      try {
        setError('')

        const response = await api.get('/analytics/customer')
        setData(response.data)
      } catch (err) {
        setError(
          extractErrorMessage(
            err,
            'Could not load customer analytics.'
          )
        )
      } finally {
        setLoading(false)
      }
    }

    loadAnalytics()
  }, [initialising, isAuthenticated])

  if (initialising || loading) {
    return <LoadingState />
  }

  if (!isAuthenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-center">
          <p className="mb-4 text-slate-600">
            You need to be logged in to view this page.
          </p>

          <Link
            to="/login"
            className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white"
          >
            Go to login
          </Link>
        </div>
      </div>
    )
  }

  return (
    <AppLayout>
      <div className="mx-auto max-w-6xl">

        {/* Header */}
        <div className="mb-8 flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">
              My Shipment Analytics
            </h1>

            <p className="mt-1 text-sm text-slate-500">
              An overview of your shipments and delivery performance.
            </p>
          </div>

          <Link
            to="/shipments"
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            View Shipments
          </Link>
        </div>

        {/* Error */}
        {error && (
          <div
            role="alert"
            className="mb-6 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700"
          >
            {error}
          </div>
        )}

        {data && (
          <>
            {/* Main Statistics */}
            <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">

              <StatCard
                label="Total Shipments"
                value={data.totalShipments ?? 0}
              />

              <StatCard
                label="Active Shipments"
                value={data.activeShipments ?? 0}
              />

              <StatCard
                label="Delivered"
                value={data.deliveredShipments ?? 0}
              />

              <StatCard
                label="Delivery Rate"
                value={`${Number(data.deliveryRate ?? 0).toFixed(0)}%`}
              />

            </div>

            {/* Status + Performance */}
            <div className="mb-6 grid gap-6 lg:grid-cols-2">

              <SectionCard title="Shipment Status Breakdown">
                <StatusBarChart
                  data={data.statusBreakdown || {}}
                />
              </SectionCard>

              <SectionCard title="Delivery Performance">

                <div className="space-y-4">

                  <div className="flex justify-between">
                    <span className="text-slate-500">
                      Delivered Shipments
                    </span>

                    <span className="font-semibold text-slate-900">
                      {data.deliveredShipments ?? 0}
                    </span>
                  </div>

                  <div className="flex justify-between">
                    <span className="text-slate-500">
                      On-Time Shipments
                    </span>

                    <span className="font-semibold text-slate-900">
                      {data.onTimeShipments ?? 0}
                    </span>
                  </div>

                  <div className="flex justify-between">
                    <span className="text-slate-500">
                      Delayed Shipments
                    </span>

                    <span className="font-semibold text-slate-900">
                      {data.delayedShipments ?? 0}
                    </span>
                  </div>

                  <div className="flex justify-between">
                    <span className="text-slate-500">
                      Cancelled Shipments
                    </span>

                    <span className="font-semibold text-slate-900">
                      {data.cancelledShipments ?? 0}
                    </span>
                  </div>

                  <div className="flex justify-between">
                    <span className="text-slate-500">
                      Delay Rate
                    </span>

                    <span className="font-semibold text-slate-900">
                      {Number(data.delayRate ?? 0).toFixed(1)}%
                    </span>
                  </div>

                  <div className="flex justify-between">
                    <span className="text-slate-500">
                      Average Delay
                    </span>

                    <span className="font-semibold text-slate-900">
                      {Number(data.averageDelayMinutes ?? 0).toFixed(0)} min
                    </span>
                  </div>

                </div>

              </SectionCard>
            </div>

            {/* Shipment Summary */}
            <SectionCard title="Shipment Summary">

              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">

                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-xs uppercase tracking-wide text-slate-500">
                    Pending
                  </p>

                  <p className="mt-2 text-xl font-bold text-slate-900">
                    {data.pendingShipments ?? 0}
                  </p>
                </div>

                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-xs uppercase tracking-wide text-slate-500">
                    In Progress
                  </p>

                  <p className="mt-2 text-xl font-bold text-slate-900">
                    {data.inProgressShipments ?? 0}
                  </p>
                </div>

                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-xs uppercase tracking-wide text-slate-500">
                    On Time
                  </p>

                  <p className="mt-2 text-xl font-bold text-slate-900">
                    {data.onTimeShipments ?? 0}
                  </p>
                </div>

                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-xs uppercase tracking-wide text-slate-500">
                    Delayed
                  </p>

                  <p className="mt-2 text-xl font-bold text-slate-900">
                    {data.delayedShipments ?? 0}
                  </p>
                </div>

              </div>

            </SectionCard>

          </>
        )}

      </div>
    </AppLayout>
  )
}