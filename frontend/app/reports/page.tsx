"use client";

import { useEffect, useState } from "react";
import { authDownload, getStoredRole, getStoredToken } from "@/lib/api";

type ReportType = {
  key: string; // matches the /api/reports/{key} path segment
  label: string;
  description: string;
};

const REPORT_TYPES: ReportType[] = [
  {
    key: "shipments",
    label: "Shipment Report",
    description: "Shipment details, status, dates, and basic information.",
  },
  {
    key: "delivery",
    label: "Delivery Report",
    description: "Delivered shipments, actual delivery dates, and proof of delivery status.",
  },
  {
    key: "route-performance",
    label: "Route Performance Report",
    description: "Distance, estimated time, and actual time per route.",
  },
  {
    key: "delay-analysis",
    label: "Delay Analysis Report",
    description: "Delay figures comparing estimated vs actual transit time.",
  },
];

export default function ReportsPage() {
  const [token, setToken] = useState<string | null | undefined>(undefined);
  const [role, setRole] = useState<string | null>(null);
  const [reportKey, setReportKey] = useState(REPORT_TYPES[0].key);
  const [format, setFormat] = useState<"pdf" | "excel">("pdf");
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    setToken(getStoredToken());
    setRole(getStoredRole());
  }, []);

  async function handleDownload() {
    setDownloading(true);
    setError(null);
    setSuccess(null);

    try {
      const report = REPORT_TYPES.find((r) => r.key === reportKey)!;
      const extension = format === "pdf" ? "pdf" : "xlsx";
      const fallbackFilename = `${reportKey}-report.${extension}`;

      await authDownload(
        `/api/reports/${reportKey}?format=${format}`,
        fallbackFilename
      );

      setSuccess(`${report.label} downloaded as ${format.toUpperCase()}.`);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setDownloading(false);
    }
  }

  if (token === undefined) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-50">
        <p className="text-zinc-600">Loading...</p>
      </div>
    );
  }

  if (token === null) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-50 px-4">
        <div className="rounded-xl border border-zinc-200 bg-white p-8 text-center shadow-sm">
          <p className="mb-4 text-zinc-700">You need to be logged in to view this page.</p>
          <a
            href="/login"
            className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white"
          >
            Go to login
          </a>
        </div>
      </div>
    );
  }

  const scopeNote =
    role === "ADMINISTRATOR"
      ? "As an admin, your reports cover platform-wide data."
      : role === "BUSINESS_CLIENT"
      ? "Your reports are restricted to your business's own shipments."
      : "Your reports are restricted to your own shipments.";

  return (
    <main className="min-h-screen bg-zinc-50 px-4 py-10">
      <div className="mx-auto max-w-2xl">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-zinc-900">Reports & Export</h1>
          <p className="mt-1 text-sm text-zinc-600">{scopeNote}</p>
        </div>

        <div className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
          {/* Report type selector */}
          <label className="mb-2 block text-sm font-medium text-zinc-700">
            Report Type
          </label>
          <div className="mb-6 space-y-2">
            {REPORT_TYPES.map((r) => (
              <label
                key={r.key}
                className={`flex cursor-pointer items-start gap-3 rounded-lg border p-3 transition ${
                  reportKey === r.key
                    ? "border-blue-500 bg-blue-50"
                    : "border-zinc-200 hover:bg-zinc-50"
                }`}
              >
                <input
                  type="radio"
                  name="reportType"
                  value={r.key}
                  checked={reportKey === r.key}
                  onChange={() => setReportKey(r.key)}
                  className="mt-1"
                />
                <div>
                  <p className="text-sm font-medium text-zinc-900">{r.label}</p>
                  <p className="text-xs text-zinc-500">{r.description}</p>
                </div>
              </label>
            ))}
          </div>

          {/* Format selector */}
          <label className="mb-2 block text-sm font-medium text-zinc-700">
            Format
          </label>
          <div className="mb-6 flex gap-3">
            {(["pdf", "excel"] as const).map((f) => (
              <button
                key={f}
                type="button"
                onClick={() => setFormat(f)}
                className={`rounded-md border px-4 py-2 text-sm font-medium ${
                  format === f
                    ? "border-zinc-900 bg-zinc-900 text-white"
                    : "border-zinc-300 text-zinc-700 hover:bg-zinc-50"
                }`}
              >
                {f === "pdf" ? "PDF" : "Excel (.xlsx)"}
              </button>
            ))}
          </div>

          {error && (
            <div className="mb-4 rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}
          {success && (
            <div className="mb-4 rounded-md bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              {success}
            </div>
          )}

          <button
            onClick={handleDownload}
            disabled={downloading}
            className="w-full rounded-md bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {downloading ? "Generating..." : "Download Report"}
          </button>
        </div>
      </div>
    </main>
  );
}
