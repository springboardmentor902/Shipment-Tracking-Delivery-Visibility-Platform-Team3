"use client";

import { useEffect, useState } from "react";
import { authGet, getStoredToken } from "@/lib/api";
import {
  StatCard,
  SectionCard,
  StatusBarChart,
  DistributionPieChart,
  LoadingState,
  LoggedOutState,
  ErrorState,
} from "../components";

type AdminAnalytics = {
  userSummary: {
    totalUsers: number;
    usersByRole: Record<string, number>;
    newUsersLast30Days: number;
  };
  shipmentMonitoring: {
    totalShipments: number;
    shipmentsByStatus: Record<string, number>;
    shipmentsCreatedLast7Days: number;
  };
  deliveryAnalytics: {
    totalDelivered: number;
    totalRejected: number;
    onTimeDeliveryRatePercent: number | null;
    averageDeliveryTimeHours: number | null;
  };
  routePerformance: {
    totalRoutes: number;
    averageDistanceKm: number | null;
    averageEstimatedTimeMinutes: number | null;
    averageActualTimeMinutes: number | null;
    averageDelayMinutes: number | null;
  };
  systemMonitoring: {
    pendingPodVerifications: number;
    activeDriversAssigned: number;
    routesInProgress: number;
  };
  reportsManagement: {
    availableReports: {
      reportType: string;
      displayName: string;
      recordCount: number;
    }[];
  };
};

export default function AdminDashboardPage() {
  const [token, setToken] = useState<string | null | undefined>(undefined);
  const [data, setData] = useState<AdminAnalytics | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setToken(getStoredToken());
  }, []);

  useEffect(() => {
    if (token === undefined) return;
    if (token === null) {
      setLoading(false);
      return;
    }

    authGet<AdminAnalytics>("/api/analytics/admin")
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [token]);

  if (token === undefined || loading) return <LoadingState />;
  if (token === null) return <LoggedOutState />;

  return (
    <main className="min-h-screen bg-zinc-50 px-4 py-10">
      <div className="mx-auto max-w-6xl">
        <div className="mb-8 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-zinc-900">Platform Analytics</h1>
            <p className="mt-1 text-sm text-zinc-600">
              Platform-wide monitoring across all users and shipments.
            </p>
          </div>
          <a
            href="/reports"
            className="rounded-md border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-800 hover:bg-zinc-100"
          >
            Reports & Export
          </a>
        </div>

        {error && <ErrorState message={error} />}

        {data && (
          <div className="space-y-6">
            {/* User Summary */}
            <SectionCard title="User Summary">
              <div className="mb-4 grid grid-cols-2 gap-4 sm:grid-cols-3">
                <StatCard label="Total Users" value={data.userSummary.totalUsers} />
                <StatCard
                  label="New (Last 30 Days)"
                  value={data.userSummary.newUsersLast30Days}
                />
                <StatCard
                  label="Roles"
                  value={Object.keys(data.userSummary.usersByRole).length}
                />
              </div>
              <DistributionPieChart data={data.userSummary.usersByRole} />
            </SectionCard>

            {/* Platform-wide Shipment Monitoring */}
            <SectionCard title="Platform-Wide Shipment Monitoring">
              <div className="mb-4 grid grid-cols-2 gap-4 sm:grid-cols-3">
                <StatCard
                  label="Total Shipments"
                  value={data.shipmentMonitoring.totalShipments}
                />
                <StatCard
                  label="Created (Last 7 Days)"
                  value={data.shipmentMonitoring.shipmentsCreatedLast7Days}
                />
                <StatCard
                  label="Routes In Progress"
                  value={data.systemMonitoring.routesInProgress}
                />
              </div>
              <StatusBarChart data={data.shipmentMonitoring.shipmentsByStatus} />
            </SectionCard>

            {/* Delivery Analytics + Route Performance */}
            <div className="grid gap-6 lg:grid-cols-2">
              <SectionCard title="Delivery Analytics">
                <div className="grid grid-cols-2 gap-4">
                  <StatCard label="Delivered" value={data.deliveryAnalytics.totalDelivered} />
                  <StatCard label="Rejected" value={data.deliveryAnalytics.totalRejected} />
                  <StatCard
                    label="On-Time Rate"
                    value={
                      data.deliveryAnalytics.onTimeDeliveryRatePercent != null
                        ? `${data.deliveryAnalytics.onTimeDeliveryRatePercent.toFixed(0)}%`
                        : "—"
                    }
                  />
                  <StatCard
                    label="Avg Delivery Time"
                    value={
                      data.deliveryAnalytics.averageDeliveryTimeHours != null
                        ? `${data.deliveryAnalytics.averageDeliveryTimeHours.toFixed(1)}h`
                        : "—"
                    }
                  />
                </div>
              </SectionCard>

              <SectionCard title="Route Performance">
                <div className="grid grid-cols-2 gap-4">
                  <StatCard label="Total Routes" value={data.routePerformance.totalRoutes} />
                  <StatCard
                    label="Avg Distance"
                    value={
                      data.routePerformance.averageDistanceKm != null
                        ? `${data.routePerformance.averageDistanceKm.toFixed(0)} km`
                        : "—"
                    }
                  />
                  <StatCard
                    label="Avg Estimated Time"
                    value={
                      data.routePerformance.averageEstimatedTimeMinutes != null
                        ? `${data.routePerformance.averageEstimatedTimeMinutes.toFixed(0)}m`
                        : "—"
                    }
                  />
                  <StatCard
                    label="Avg Delay"
                    value={
                      data.routePerformance.averageDelayMinutes != null
                        ? `${data.routePerformance.averageDelayMinutes.toFixed(0)}m`
                        : "—"
                    }
                  />
                </div>
              </SectionCard>
            </div>

            {/* System Monitoring */}
            <SectionCard title="System Monitoring">
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                <StatCard
                  label="Pending POD Verifications"
                  value={data.systemMonitoring.pendingPodVerifications}
                />
                <StatCard
                  label="Active Drivers Assigned"
                  value={data.systemMonitoring.activeDriversAssigned}
                />
                <StatCard
                  label="Routes In Progress"
                  value={data.systemMonitoring.routesInProgress}
                />
              </div>
              {data.systemMonitoring.pendingPodVerifications > 0 && (
                <a
                  href="/admin/verification"
                  className="mt-4 inline-block text-sm font-medium text-blue-700 hover:underline"
                >
                  Go to Verification Queue →
                </a>
              )}
            </SectionCard>

            {/* Reports Management */}
            <SectionCard title="Reports Management">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="text-xs uppercase text-zinc-500">
                    <tr>
                      <th className="py-2 pr-4">Report</th>
                      <th className="py-2 pr-4">Available Records</th>
                      <th className="py-2 pr-4"></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-100">
                    {data.reportsManagement.availableReports.map((r) => (
                      <tr key={r.reportType}>
                        <td className="py-2 pr-4 font-medium text-zinc-900">
                          {r.displayName}
                        </td>
                        <td className="py-2 pr-4 text-zinc-600">{r.recordCount}</td>
                        <td className="py-2 pr-4 text-right">
                          <a
                            href="/reports"
                            className="rounded-md border border-zinc-300 px-3 py-1.5 text-xs font-medium text-zinc-800 hover:bg-zinc-100"
                          >
                            Export
                          </a>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </SectionCard>
          </div>
        )}
      </div>
    </main>
  );
}
