"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";

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

export default function ShipmentDetailsPage() {
  const params = useParams();
  const router = useRouter();

  const trackingNumber = params.trackingNumber as string;

  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const token = localStorage.getItem("shiptrack_token");

    if (!token) {
      router.push("/login");
      return;
    }

    const fetchShipment = async () => {
      try {
        const response = await fetch(
          `http://localhost:8080/api/shipments/${trackingNumber}`,
          {
            method: "GET",
            headers: {
              Authorization: `Bearer ${token}`,
              "Content-Type": "application/json",
            },
          }
        );

        if (!response.ok) {
          throw new Error("Shipment not found");
        }

        const data = await response.json();
        setShipment(data);
      } catch (err) {
        setError("Unable to load shipment details.");
      } finally {
        setLoading(false);
      }
    };

    fetchShipment();
  }, [trackingNumber, router]);

  if (loading) {
    return (
      <main className="min-h-screen flex items-center justify-center">
        <p className="text-zinc-600">Loading shipment...</p>
      </main>
    );
  }

  if (error || !shipment) {
    return (
      <main className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-zinc-900">
            Shipment Not Found
          </h1>

          <p className="mt-2 text-zinc-500">
            {error}
          </p>

          <button
            onClick={() => router.push("/shipments")}
            className="mt-6 rounded-md bg-zinc-900 px-5 py-2 text-white"
          >
            Back to My Shipments
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-zinc-50 px-6 py-10">
      <div className="mx-auto max-w-3xl">

        <button
          onClick={() => router.push("/shipments")}
          className="mb-6 text-sm text-zinc-600 hover:text-zinc-900"
        >
          ← Back to My Shipments
        </button>

        <div className="rounded-xl border border-zinc-200 bg-white p-8 shadow-sm">

          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-zinc-500">
                Tracking Number
              </p>

              <h1 className="text-2xl font-bold text-zinc-900">
                {shipment.trackingNumber}
              </h1>
            </div>

            <span className="rounded-full bg-blue-50 px-4 py-2 text-sm font-medium text-blue-700">
              {shipment.status}
            </span>
          </div>

          <div className="mt-8 grid gap-6 md:grid-cols-2">

            <div>
              <p className="text-sm text-zinc-500">
                Sender
              </p>

              <p className="mt-1 font-medium">
                {shipment.senderName}
              </p>

              <p className="text-sm text-zinc-600">
                {shipment.senderAddress}
              </p>
            </div>

            <div>
              <p className="text-sm text-zinc-500">
                Receiver
              </p>

              <p className="mt-1 font-medium">
                {shipment.receiverName}
              </p>

              <p className="text-sm text-zinc-600">
                {shipment.receiverAddress}
              </p>
            </div>

            <div>
              <p className="text-sm text-zinc-500">
                Receiver Phone
              </p>

              <p className="mt-1 font-medium">
                {shipment.receiverPhone}
              </p>
            </div>

            <div>
              <p className="text-sm text-zinc-500">
                Weight
              </p>

              <p className="mt-1 font-medium">
                {shipment.weightKg} kg
              </p>
            </div>

            <div className="md:col-span-2">
              <p className="text-sm text-zinc-500">
                Package Description
              </p>

              <p className="mt-1 font-medium">
                {shipment.packageDescription}
              </p>
            </div>

            <div className="md:col-span-2">
              <p className="text-sm text-zinc-500">
                Created At
              </p>

              <p className="mt-1 font-medium">
                {new Date(shipment.createdAt).toLocaleString()}
              </p>
            </div>

          </div>
        </div>
      </div>
    </main>
  );
}