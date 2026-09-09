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

type BusinessAnalytics = {
  totalShipments: number;
  activeShipments: number;
  deliveredShipments: number;
  statusBreakdown: Record<string, number>;
  deliveryPerformance: {
    onTimeDeliveryRatePercent: number | null;
    averageDeliveryTimeHours: number | null;
  };
  delayAnalysis: {
    delayedShipmentCount: number;
    averageDelayMinutes: number | null;
  };
  customerActivity: {
    distinctReceiverCount: number;
    topReceivers: Record<string, number>;
  };
  logisticsOverview: {
    shipmentCountByStatus: Record<string, number>;
    totalDistanceCoveredKm: number | null;
    routesWithDriverAssigned: number;
    totalRoutes: number;
  };
};

export default function BusinessDashboardPage() {
  const [token, setToken] = useState<string | null | undefined>(undefined);
  const [data, setData] = useState<BusinessAnalytics | null>(null);
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

    authGet<BusinessAnalytics>("/api/analytics/business")
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
            <h1 className="text-2xl font-bold text-zinc-900">Business Analytics</h1>
            <p className="mt-1 text-sm text-zinc-600">
              Logistics and delivery performance for your business's shipments only.
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
            <div className="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
              <StatCard label="Total Shipments" value={data.totalShipments} />
              <StatCard label="Active" value={data.activeShipments} />
              <StatCard label="Delivered" value={data.deliveredShipments} />
              <StatCard
                label="On-Time Rate"
                value={
                  data.deliveryPerformance.onTimeDeliveryRatePercent != null
                    ? `${data.deliveryPerformance.onTimeDeliveryRatePercent.toFixed(0)}%`
                    : "—"
                }
              />
              <StatCard
                label="Avg Delivery Time"
                value={
                  data.deliveryPerformance.averageDeliveryTimeHours != null
                    ? `${data.deliveryPerformance.averageDeliveryTimeHours.toFixed(1)}h`
                    : "—"
                }
              />
              <StatCard
                label="Avg Delay"
                value={
                  data.delayAnalysis.averageDelayMinutes != null
                    ? `${data.delayAnalysis.averageDelayMinutes.toFixed(0)}m`
                    : "—"
                }
                hint={`${data.delayAnalysis.delayedShipmentCount} delayed`}
              />
            </div>

            <div className="mb-6 grid gap-6 lg:grid-cols-2">
              <SectionCard title="Shipment Status Breakdown">
                <StatusBarChart data={data.statusBreakdown} />
              </SectionCard>

              <SectionCard title="Top Receivers (Customer Activity)">
                <div className="mb-3 text-sm text-zinc-600">
                  {data.customerActivity.distinctReceiverCount} distinct receivers
                </div>
                <ul className="space-y-2">
                  {Object.entries(data.customerActivity.topReceivers).map(
                    ([name, count]) => (
                      <li
                        key={name}
                        className="flex items-center justify-between rounded-md bg-zinc-50 px-3 py-2 text-sm"
                      >
                        <span className="text-zinc-700">{name}</span>
                        <span className="font-semibold text-zinc-900">{count}</span>
                      </li>
                    )
                  )}
                  {Object.keys(data.customerActivity.topReceivers).length === 0 && (
                    <p className="text-sm text-zinc-400">No receiver activity yet.</p>
                  )}
                </ul>
              </SectionCard>
            </div>

            <SectionCard title="Logistics Overview">
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                <StatCard
                  label="Total Routes"
                  value={data.logisticsOverview.totalRoutes}
                />
                <StatCard
                  label="Routes With Driver"
                  value={data.logisticsOverview.routesWithDriverAssigned}
                />
                <StatCard
                  label="Distance Covered"
                  value={
                    data.logisticsOverview.totalDistanceCoveredKm != null
                      ? `${data.logisticsOverview.totalDistanceCoveredKm.toFixed(0)} km`
                      : "—"
                  }
                />
              </div>
            </SectionCard>
          </>
        )}
      </div>
    </main>
  );
}
