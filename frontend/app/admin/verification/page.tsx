"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { API_BASE_URL, getStoredToken } from "@/lib/api";

type PodSummary = {
  id: number;
  shipmentId: number;
  trackingNumber: string;
  receiverName: string | null;
  submittedByName: string | null;
  status: string;
  submittedAt: string;
};

export default function VerificationQueuePage() {
  const [token, setToken] = useState<string | null | undefined>(undefined);
  const [queue, setQueue] = useState<PodSummary[]>([]);
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

    loadQueue(token);
  }, [token]);

  async function loadQueue(authToken: string) {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`${API_BASE_URL}/api/pod/pending`, {
        method: "GET",
        headers: { Authorization: `Bearer ${authToken}` },
      });

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        setError(data?.message || `Request failed (status ${response.status})`);
        return;
      }

      setQueue(data);
    } catch {
      setError("Could not reach the backend. Is it running on port 8080?");
    } finally {
      setLoading(false);
    }
  }

  if (token === undefined || loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-50">
        <p className="text-zinc-600">Loading verification queue...</p>
      </div>
    );
  }

  if (token === null) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-50 px-4">
        <div className="rounded-xl border border-zinc-200 bg-white p-8 text-center shadow-sm">
          <p className="mb-4 text-zinc-700">
            You need to be logged in as support/admin to view this page.
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
      <div className="mx-auto max-w-5xl">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-zinc-900">
            Verification Queue
          </h1>
          <p className="mt-1 text-sm text-zinc-600">
            Proofs of delivery waiting for approval or rejection.
          </p>
        </div>

        {error && (
          <div className="mb-6 rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {!error && queue.length === 0 ? (
          <div className="rounded-xl border border-zinc-200 bg-white p-10 text-center">
            <h2 className="text-lg font-semibold text-zinc-900">
              Nothing pending
            </h2>
            <p className="mt-2 text-sm text-zinc-600">
              All submitted proofs of delivery have been reviewed.
            </p>
          </div>
        ) : (
          <div className="overflow-hidden rounded-xl border border-zinc-200 bg-white shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="bg-zinc-50 text-xs uppercase text-zinc-500">
                <tr>
                  <th className="px-5 py-3">Tracking Number</th>
                  <th className="px-5 py-3">Receiver</th>
                  <th className="px-5 py-3">Submitted By</th>
                  <th className="px-5 py-3">Submitted At</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100">
                {queue.map((pod) => (
                  <tr key={pod.id} className="hover:bg-zinc-50">
                    <td className="px-5 py-4 font-medium text-zinc-900">
                      {pod.trackingNumber}
                    </td>
                    <td className="px-5 py-4 text-zinc-700">
                      {pod.receiverName || "—"}
                    </td>
                    <td className="px-5 py-4 text-zinc-700">
                      {pod.submittedByName || "—"}
                    </td>
                    <td className="px-5 py-4 text-zinc-500">
                      {new Date(pod.submittedAt).toLocaleString()}
                    </td>
                    <td className="px-5 py-4">
                      <span className="rounded-full bg-amber-50 px-3 py-1 text-xs font-medium text-amber-700">
                        {pod.status}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-right">
                      <Link
                        href={`/admin/verification/${pod.shipmentId}`}
                        className="rounded-md border border-zinc-300 px-3 py-1.5 text-xs font-medium text-zinc-800 hover:bg-zinc-100"
                      >
                        Review
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </main>
  );
}
