"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { API_BASE_URL, getStoredToken } from "@/lib/api";

type ShipmentForm = {
  senderName: string;
  senderAddress: string;
  receiverName: string;
  receiverAddress: string;
  receiverPhone: string;
  packageDescription: string;
  weightKg: string;
};

const emptyForm: ShipmentForm = {
  senderName: "",
  senderAddress: "",
  receiverName: "",
  receiverAddress: "",
  receiverPhone: "",
  packageDescription: "",
  weightKg: "",
};

export default function NewShipmentPage() {
  const [token, setToken] = useState<string | null | undefined>(undefined);
  const [form, setForm] = useState<ShipmentForm>(emptyForm);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<{
    trackingNumber: string;
    status: string;
  } | null>(null);

  useEffect(() => {
    setToken(getStoredToken());
  }, []);

  function updateField(field: keyof ShipmentForm, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!token) {
      setError("You must be logged in to create a shipment.");
      return;
    }

    setSubmitting(true);
    try {
      const res = await fetch(`${API_BASE_URL}/api/shipments`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          ...form,
          weightKg: form.weightKg === "" ? null : Number(form.weightKg),
        }),
      });

      const data = await res.json().catch(() => null);

      if (!res.ok) {
        // Backend validation errors typically come back as a field->message map,
        // or as a { message } object for business-rule errors.
        if (data && typeof data === "object" && !data.message) {
          const messages = Object.values(data).join(" | ");
          setError(messages || `Request failed (status ${res.status})`);
        } else {
          setError(data?.message || `Request failed (status ${res.status})`);
        }
        return;
      }

      setSuccess({ trackingNumber: data.trackingNumber, status: data.status });
      setForm(emptyForm);
    } catch (err) {
      setError("Could not reach the backend. Is it running on port 8080?");
    } finally {
      setSubmitting(false);
    }
  }

  if (token === undefined) {
    // Avoids a flash of the "please log in" message during initial hydration.
    return null;
  }

  if (token === null) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-50 px-4">
        <div className="rounded-xl border border-zinc-200 bg-white p-8 text-center shadow-sm">
          <p className="mb-4 text-zinc-700">
            You need to be logged in to create a shipment.
          </p>
          <Link
            href="/login"
            className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-800"
          >
            Go to login
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen justify-center bg-zinc-50 px-4 py-12">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-lg rounded-xl border border-zinc-200 bg-white p-8 shadow-sm"
      >
        <h1 className="mb-6 text-xl font-semibold text-zinc-900">
          Create a Shipment
        </h1>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field
            label="Sender name"
            value={form.senderName}
            onChange={(v) => updateField("senderName", v)}
          />
          <Field
            label="Sender address"
            value={form.senderAddress}
            onChange={(v) => updateField("senderAddress", v)}
          />
          <Field
            label="Receiver name"
            value={form.receiverName}
            onChange={(v) => updateField("receiverName", v)}
          />
          <Field
            label="Receiver address"
            value={form.receiverAddress}
            onChange={(v) => updateField("receiverAddress", v)}
          />
          <Field
            label="Receiver phone"
            value={form.receiverPhone}
            onChange={(v) => updateField("receiverPhone", v)}
          />
          <Field
            label="Weight (kg)"
            type="number"
            value={form.weightKg}
            onChange={(v) => updateField("weightKg", v)}
          />
        </div>

        <label className="mb-1 mt-4 block text-sm font-medium text-zinc-700">
          Package description
        </label>
        <textarea
          required
          value={form.packageDescription}
          onChange={(e) => updateField("packageDescription", e.target.value)}
          className="mb-4 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none"
          rows={3}
        />

        {error && (
          <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}

        {success && (
          <p className="mb-4 rounded-md bg-green-50 px-3 py-2 text-sm text-green-700">
            Shipment created! Tracking number:{" "}
            <span className="font-semibold">{success.trackingNumber}</span>{" "}
            (status: {success.status})
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-800 disabled:opacity-50"
        >
          {submitting ? "Creating..." : "Create shipment"}
        </button>
      </form>
    </div>
  );
}

function Field({
  label,
  value,
  onChange,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
}) {
  return (
    <div>
      <label className="mb-1 block text-sm font-medium text-zinc-700">
        {label}
      </label>
      <input
        type={type}
        required
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none"
      />
    </div>
  );
}
