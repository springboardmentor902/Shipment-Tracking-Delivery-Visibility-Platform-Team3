"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { API_BASE_URL, getStoredToken } from "@/lib/api";

type PodDetail = {
  id: number;
  shipmentId: number;
  trackingNumber: string;
  receiverName: string | null;
  notes: string | null;
  signatureImage: string;
  photoImage: string;
  submittedByName: string | null;
  status: string;
  submittedAt: string;
  verifiedByName: string | null;
  verifiedAt: string | null;
  rejectionReason: string | null;
};

export default function VerificationDetailPage() {
  const params = useParams<{ shipmentId: string }>();
  const router = useRouter();
  const shipmentId = params.shipmentId;

  const [token, setToken] = useState<string | null | undefined>(undefined);
  const [pod, setPod] = useState<PodDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [showRejectForm, setShowRejectForm] = useState(false);
  const [rejectionReason, setRejectionReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    setToken(getStoredToken());
  }, []);

  useEffect(() => {
    if (token === undefined) return;

    if (token === null) {
      setLoading(false);
      return;
    }

    loadPod(token);
  }, [token, shipmentId]);

  async function loadPod(authToken: string) {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/pod/${shipmentId}`,
        {
          method: "GET",
          headers: { Authorization: `Bearer ${authToken}` },
        }
      );

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        setError(data?.message || `Request failed (status ${response.status})`);
        return;
      }

      setPod(data);
    } catch {
      setError("Could not reach the backend. Is it running on port 8080?");
    } finally {
      setLoading(false);
    }
  }

  async function handleVerify(decision: "APPROVED" | "REJECTED") {
    if (!token) return;

    if (decision === "REJECTED" && !rejectionReason.trim()) {
      setActionError("A rejection reason is required.");
      return;
    }

    setSubmitting(true);
    setActionError(null);

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/pod/${shipmentId}/verify`,
        {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            decision,
            rejectionReason: decision === "REJECTED" ? rejectionReason : undefined,
          }),
        }
      );

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        setActionError(data?.message || `Request failed (status ${response.status})`);
        return;
      }

      // Back to the queue - this proof is no longer pending.
      router.push("/admin/verification");
    } catch {
      setActionError("Could not reach the backend. Is it running on port 8080?");
    } finally {
      setSubmitting(false);
    }
  }

  if (token === undefined || loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-50">
        <p className="text-zinc-600">Loading proof of delivery...</p>
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

  if (error || !pod) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-50 px-4">
        <div className="rounded-xl border border-zinc-200 bg-white p-8 text-center shadow-sm">
          <p className="text-red-700">{error || "Proof of delivery not found."}</p>
          <Link
            href="/admin/verification"
            className="mt-4 inline-block rounded-md border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-800 hover:bg-zinc-50"
          >
            Back to queue
          </Link>
        </div>
      </div>
    );
  }

  const isPending = pod.status === "PENDING";

  return (
    <main className="min-h-screen bg-zinc-50 px-4 py-10">
      <div className="mx-auto max-w-4xl">
        <Link
          href="/admin/verification"
          className="mb-6 inline-block text-sm text-zinc-500 hover:text-zinc-800"
        >
          ← Back to queue
        </Link>

        <div className="rounded-xl border border-zinc-200 bg-white p-8 shadow-sm">
          <div className="mb-6 flex items-start justify-between">
            <div>
              <p className="text-xs text-zinc-500">Tracking Number</p>
              <h1 className="text-xl font-bold text-zinc-900">
                {pod.trackingNumber}
              </h1>
            </div>

            <span
              className={`rounded-full px-3 py-1 text-xs font-medium ${
                pod.status === "PENDING"
                  ? "bg-amber-50 text-amber-700"
                  : pod.status === "APPROVED"
                  ? "bg-emerald-50 text-emerald-700"
                  : "bg-red-50 text-red-700"
              }`}
            >
              {pod.status}
            </span>
          </div>

          <div className="mb-8 grid gap-4 text-sm sm:grid-cols-2">
            <div>
              <p className="text-xs text-zinc-500">Receiver Name</p>
              <p className="text-zinc-800">{pod.receiverName || "—"}</p>
            </div>
            <div>
              <p className="text-xs text-zinc-500">Submitted By</p>
              <p className="text-zinc-800">{pod.submittedByName || "—"}</p>
            </div>
            <div>
              <p className="text-xs text-zinc-500">Submitted At</p>
              <p className="text-zinc-800">
                {new Date(pod.submittedAt).toLocaleString()}
              </p>
            </div>
            {pod.notes && (
              <div className="sm:col-span-2">
                <p className="text-xs text-zinc-500">Driver Notes</p>
                <p className="text-zinc-800">{pod.notes}</p>
              </div>
            )}
          </div>

          <div className="mb-8 grid gap-6 sm:grid-cols-2">
            <div>
              <p className="mb-2 text-xs font-medium uppercase text-zinc-500">
                Recipient Signature
              </p>
              <div className="overflow-hidden rounded-lg border border-zinc-200 bg-white">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={pod.signatureImage}
                  alt="Recipient signature"
                  className="h-56 w-full object-contain"
                />
              </div>
            </div>

            <div>
              <p className="mb-2 text-xs font-medium uppercase text-zinc-500">
                Delivery Photo
              </p>
              <div className="overflow-hidden rounded-lg border border-zinc-200 bg-zinc-100">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={pod.photoImage}
                  alt="Delivery photo"
                  className="h-56 w-full object-cover"
                />
              </div>
            </div>
          </div>

          {!isPending && (
            <div className="rounded-lg bg-zinc-50 p-4 text-sm text-zinc-600">
              {pod.status === "APPROVED" ? "Approved" : "Rejected"} by{" "}
              {pod.verifiedByName || "—"} on{" "}
              {pod.verifiedAt && new Date(pod.verifiedAt).toLocaleString()}
              {pod.rejectionReason && (
                <p className="mt-1 text-red-700">
                  Reason: {pod.rejectionReason}
                </p>
              )}
            </div>
          )}

          {isPending && (
            <div className="border-t border-zinc-200 pt-6">
              {actionError && (
                <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
                  {actionError}
                </p>
              )}

              {showRejectForm ? (
                <div className="space-y-3">
                  <label className="block text-sm font-medium text-zinc-700">
                    Rejection reason
                  </label>
                  <textarea
                    value={rejectionReason}
                    onChange={(e) => setRejectionReason(e.target.value)}
                    rows={3}
                    className="w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none"
                    placeholder="e.g. Signature does not match records, photo is unclear..."
                  />
                  <div className="flex gap-3">
                    <button
                      onClick={() => handleVerify("REJECTED")}
                      disabled={submitting}
                      className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
                    >
                      {submitting ? "Rejecting..." : "Confirm Rejection"}
                    </button>
                    <button
                      onClick={() => {
                        setShowRejectForm(false);
                        setActionError(null);
                      }}
                      disabled={submitting}
                      className="rounded-md border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-800 hover:bg-zinc-50"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <div className="flex gap-3">
                  <button
                    onClick={() => handleVerify("APPROVED")}
                    disabled={submitting}
                    className="rounded-md bg-emerald-600 px-5 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
                  >
                    {submitting ? "Approving..." : "Approve"}
                  </button>
                  <button
                    onClick={() => setShowRejectForm(true)}
                    disabled={submitting}
                    className="rounded-md border border-red-300 px-5 py-2 text-sm font-medium text-red-700 hover:bg-red-50"
                  >
                    Reject
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </main>
  );
}
