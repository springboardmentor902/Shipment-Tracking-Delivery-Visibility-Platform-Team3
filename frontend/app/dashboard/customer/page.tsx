"use client";

import { useEffect, useState } from "react";
import { authGet, getStoredToken } from "@/lib/api";
import {
  StatCard,
  SectionCard,
  StatusBarChart,
  LoadingState,
  LoggedOutState,
  ErrorState,
} from "../components";

type CustomerAnalytics = {
  activeShipmentCount: number;
  totalShipmentCount: number;
  statusBreakdown: Record<string, number>;
  shipmentHistory: {
    trackingNumber: string;
    status: string;
    receiverName: string;
    createdAt: string;
  }[];
  trackingInsights: {
    mostRecentStatus: string | null;
    mostRecentTrackingNumber: string | null;
    onTimeDeliveryRatePercent: number | null;
    averageDeliveryTimeHours: number | null;
  };
};

export default function CustomerDashboardPage() {
  const [token, setToken] = useState<string | null | undefined>(undefined);
  const [data, setData] = useState<CustomerAnalytics | null>(null);
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

    authGet<CustomerAnalytics>("/api/analytics/customer")
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [token]);

  if (token === undefined || loading) return <LoadingState />;
  if (token === null) return <LoggedOutState />;

  return (
    <main className="min-h-screen bg-zinc-50 px-4 py-10">
      <div className="mx-auto max-w-5xl">
        <div className="mb-8 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-zinc-900">My Shipment Analytics</h1>
            <p className="mt-1 text-sm text-zinc-600">
              An overview of your shipments and delivery performance.
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
          <>
            <div className="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
              <StatCard label="Active Shipments" value={data.activeShipmentCount} />
              <StatCard label="Total Shipments" value={data.totalShipmentCount} />
              <StatCard
                label="On-Time Rate"
                value={
                  data.trackingInsights.onTimeDeliveryRatePercent != null
                    ? `${data.trackingInsights.onTimeDeliveryRatePercent.toFixed(0)}%`
                    : "—"
                }
              />
              <StatCard
                label="Avg Delivery Time"
                value={
                  data.trackingInsights.averageDeliveryTimeHours != null
                    ? `${data.trackingInsights.averageDeliveryTimeHours.toFixed(1)}h`
                    : "—"
                }
              />
            </div>

            <div className="mb-6 grid gap-6 lg:grid-cols-2">
              <SectionCard title="Shipment Status Breakdown">
                <StatusBarChart data={data.statusBreakdown} />
              </SectionCard>

              <SectionCard title="Tracking Insights">
                <dl className="space-y-3 text-sm">
                  <div className="flex justify-between">
                    <dt className="text-zinc-500">Most Recent Shipment</dt>
                    <dd className="font-medium text-zinc-800">
                      {data.trackingInsights.mostRecentTrackingNumber || "—"}
                    </dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-zinc-500">Current Status</dt>
                    <dd className="font-medium text-zinc-800">
                      {data.trackingInsights.mostRecentStatus || "—"}
                    </dd>
                  </div>
                </dl>
              </SectionCard>
            </div>

            <SectionCard title="Shipment History">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="text-xs uppercase text-zinc-500">
                    <tr>
                      <th className="py-2 pr-4">Tracking Number</th>
                      <th className="py-2 pr-4">Receiver</th>
                      <th className="py-2 pr-4">Status</th>
                      <th className="py-2 pr-4">Created</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-100">
                    {data.shipmentHistory.length === 0 && (
                      <tr>
                        <td colSpan={4} className="py-6 text-center text-zinc-400">
                          No shipments yet.
                        </td>
                      </tr>
                    )}
                    {data.shipmentHistory.map((s) => (
                      <tr key={s.trackingNumber}>
                        <td className="py-2 pr-4 font-medium text-zinc-900">
                          {s.trackingNumber}
                        </td>
                        <td className="py-2 pr-4 text-zinc-700">{s.receiverName}</td>
                        <td className="py-2 pr-4">
                          <span className="rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700">
                            {s.status}
                          </span>
                        </td>
                        <td className="py-2 pr-4 text-zinc-500">
                          {new Date(s.createdAt).toLocaleDateString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </SectionCard>
          </>
        )}
      </div>
    </main>
  );
}
