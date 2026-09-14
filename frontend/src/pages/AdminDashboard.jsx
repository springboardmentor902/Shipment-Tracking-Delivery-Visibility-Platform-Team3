import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
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

export default function AdminDashboard() {
  const { isAuthenticated, initialising } = useAuth()
const navigate = useNavigate()
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

        const response = await api.get('/analytics/admin')
        setData(response.data)
      } catch (err) {
        setError(
          extractErrorMessage(
            err,
            'Could not load admin analytics.'
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
              Admin Analytics
            </h1>

            <p className="mt-1 text-sm text-slate-500">
              Platform-wide shipment, delivery, route and system overview.
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
            {/* User Summary */}
            <SectionCard title="User Summary">
              <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">

                <StatCard
                  label="Total Users"
                  value={data.totalUsers ?? 0}
                />
<button onClick={() => navigate('/reports')}>
  Reports
</button>
                <StatCard
                  label="Customers"
                  value={data.totalCustomers ?? 0}
                />

                <StatCard
                  label="Business Clients"
                  value={data.totalBusinessClients ?? 0}
                />

                <StatCard
                  label="Active Users"
                  value={data.activeUsers ?? 0}
                />

              </div>
            </SectionCard>

            <div className="mb-6" />

            {/* Shipment Monitoring */}
            <SectionCard title="Platform Shipment Monitoring">
              <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">

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
                  label="Cancelled"
                  value={data.cancelledShipments ?? 0}
                />

              </div>
            </SectionCard>

            <div className="mb-6" />

            {/* Status + Delivery */}
            <div className="mb-6 grid gap-6 lg:grid-cols-2">

              <SectionCard title="Shipment Status Breakdown">
                <StatusBarChart
                  data={data.statusBreakdown || {}}
                />
              </SectionCard>

              <SectionCard title="Delivery Analytics">
                <div className="space-y-4">

                  <div className="flex justify-between">
                    <span className="text-slate-500">
                      Delivery Rate
                    </span>

                    <span className="font-semibold text-slate-900">
                      {Number(data.deliveryRate ?? 0).toFixed(1)}%
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

            {/* Route Performance */}
            <SectionCard title="Route Performance">

              <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">

                <StatCard
                  label="Total Routes"
                  value={data.totalRoutes ?? 0}
                />

                <StatCard
                  label="Total Distance"
                  value={`${Number(
                    data.totalDistanceKm ?? 0
                  ).toFixed(1)} km`}
                />

                <StatCard
                  label="On-Time Routes"
                  value={data.onTimeRoutes ?? 0}
                />

                <StatCard
                  label="Delayed Routes"
                  value={data.delayedRoutes ?? 0}
                />

              </div>

            </SectionCard>

            <div className="mb-6" />

            {/* System Monitoring */}
            <SectionCard title="System Monitoring">

              <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">

                <StatCard
                  label="Active Users"
                  value={data.activeUsers ?? 0}
                />

                <StatCard
                  label="Active Routes"
                  value={data.activeRoutes ?? 0}
                />

                <StatCard
                  label="Total Reports"
                  value={data.totalReports ?? 0}
                />

                <StatCard
                  label="Administrators"
                  value={data.totalAdministrators ?? 0}
                />

              </div>

            </SectionCard>

            <div className="mb-6" />

            {/* Reports Management */}
            <SectionCard title="Reports Management">

              <div className="flex flex-wrap items-center justify-between gap-4">

                <div>
                  <p className="font-medium text-slate-900">
                    Available Reports
                  </p>

                  <p className="mt-1 text-sm text-slate-500">
                    Shipment, delivery, route and logistics reporting.
                  </p>
                </div>

                <Link
                  to="/reports"
                  className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
                >
                  Reports
                </Link>

              </div>

            </SectionCard>

          </>
        )}

      </div>
    </AppLayout>
  )
}