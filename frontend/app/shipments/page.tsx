"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { API_BASE_URL, getStoredToken } from "@/lib/api";

type Shipment = {
  id: number;
  trackingNumber: string;
  customerId: number;
  customerEmail: string;
  senderName: string;
  senderAddress: string;
  receiverName: string;
  receiverAddress: string;
  receiverPhone: string;
  packageDescription: string;
  weightKg: number;
  status: string;
  createdAt: string;
};

export default function ShipmentsPage() {
  const [token, setToken] = useState<string | null | undefined>(undefined);
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setToken(getStoredToken());
  }, []);

  useEffect(() => {
    if (token === undefined) return;

    if (token === null) {
      setLoading(false);
      return;
    }

    async function loadShipments() {
      try {
        const response = await fetch(`${API_BASE_URL}/api/shipments`, {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        const data = await response.json().catch(() => null);

        if (!response.ok) {
          setError(
            data?.message || `Request failed (status ${response.status})`
          );
          return;
        }

        setShipments(data);
      } catch {
        setError("Could not reach the backend. Is it running on port 8080?");
      } finally {
        setLoading(false);
      }
    }

    loadShipments();
  }, [token]);

  if (token === undefined || loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-50">
        <p className="text-zinc-600">Loading shipments...</p>
      </div>
    );
  }

  if (token === null) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-50 px-4">
        <div className="rounded-xl border border-zinc-200 bg-white p-8 text-center shadow-sm">
          <p className="mb-4 text-zinc-700">
            You need to be logged in to view your shipments.
          </p>

          <Link
            href="/login"
            className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white"
          >
            Go to login
          </Link>
        </div>
      </div>
    );
  }

  return (
    <main className="min-h-screen bg-zinc-50 px-4 py-10">
      <div className="mx-auto max-w-6xl">
        <div className="mb-8 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-zinc-900">
              My Shipments
            </h1>

            <p className="mt-1 text-sm text-zinc-600">
              View and track your shipments.
            </p>
          </div>

          <Link
            href="/shipments/new"
            className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-800"
          >
            + New Shipment
          </Link>
        </div>

        {error && (
          <div className="mb-6 rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {shipments.length === 0 && !error ? (
          <div className="rounded-xl border border-zinc-200 bg-white p-10 text-center">
            <h2 className="text-lg font-semibold text-zinc-900">
              No shipments yet
            </h2>

            <p className="mt-2 text-sm text-zinc-600">
              Create your first shipment to get started.
            </p>

            <Link
              href="/shipments/new"
              className="mt-5 inline-block rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white"
            >
              Create Shipment
            </Link>
          </div>
        ) : (
          <div className="grid gap-5 md:grid-cols-2">
            {shipments.map((shipment) => (
              <div
                key={shipment.id}
                className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm"
              >
                <div className="mb-4 flex items-center justify-between">
                  <div>
                    <p className="text-xs text-zinc-500">
                      Tracking Number
                    </p>

                    <p className="font-semibold text-zinc-900">
                      {shipment.trackingNumber}
                    </p>
                  </div>

                  <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700">
                    {shipment.status}
                  </span>
                </div>

                <div className="space-y-3 text-sm">
                  <div>
                    <p className="text-xs text-zinc-500">From</p>
                    <p className="text-zinc-800">
                      {shipment.senderName}
                    </p>
                    <p className="text-zinc-500">
                      {shipment.senderAddress}
                    </p>
                  </div>

                  <div>
                    <p className="text-xs text-zinc-500">To</p>
                    <p className="text-zinc-800">
                      {shipment.receiverName}
                    </p>
                    <p className="text-zinc-500">
                      {shipment.receiverAddress}
                    </p>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-xs text-zinc-500">Package</p>
                      <p className="text-zinc-800">
                        {shipment.packageDescription}
                      </p>
                    </div>

                    <div>
                      <p className="text-xs text-zinc-500">Weight</p>
                      <p className="text-zinc-800">
                        {shipment.weightKg} kg
                      </p>
                    </div>
                  </div>
                </div>

                <Link
                  href={`/shipments/${shipment.trackingNumber}`}
                  className="mt-5 block w-full rounded-md border border-zinc-300 px-4 py-2 text-center text-sm font-medium text-zinc-800 hover:bg-zinc-50"
                >
                  View Shipment
                </Link>
              </div>
            ))}
          </div>
        )}
      </div>
    </main>
  );
}