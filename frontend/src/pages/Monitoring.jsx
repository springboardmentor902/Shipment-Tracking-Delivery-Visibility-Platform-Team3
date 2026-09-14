import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import api from "../lib/api";

function formatValue(value) {
  if (value === null || value === undefined || value === "") {
    return "—";
  }

  return value;
}

function getShipmentId(shipment) {
  return (
    shipment?.id ??
    shipment?.shipmentId ??
    shipment?.shipment_id ??
    null
  );
}

function getRouteId(shipment) {
  return (
    shipment?.routeId ??
    shipment?.route_id ??
    shipment?.assignedRouteId ??
    shipment?.assignedRoute?.id ??
    shipment?.route?.routeId ??
    shipment?.route?.id ??
    shipment?.currentRoute?.id ??
    shipment?.currentRoute?.routeId ??
    null
  );
}

function getStatus(shipment) {
  const status =
    shipment?.status ??
    shipment?.shipmentStatus ??
    shipment?.route?.status ??
    "UNKNOWN";

  return String(status).trim().toUpperCase();
}

function getLocation(shipment) {
  return (
    shipment?.lastLocation ??
    shipment?.currentLocation ??
    shipment?.location ??
    shipment?.route?.lastLocation ??
    ""
  );
}

function getLatitude(shipment) {
  return (
    shipment?.latitude ??
    shipment?.lastLatitude ??
    shipment?.currentLatitude ??
    shipment?.route?.latitude ??
    ""
  );
}

function getLongitude(shipment) {
  return (
    shipment?.longitude ??
    shipment?.lastLongitude ??
    shipment?.currentLongitude ??
    shipment?.route?.longitude ??
    ""
  );
}

function getStatusClass(status) {
  switch (status) {
    case "IN_TRANSIT":
    case "IN PROGRESS":
    case "ONGOING":
      return "bg-blue-100 text-blue-700";

    case "DELIVERED":
    case "COMPLETED":
      return "bg-green-100 text-green-700";

    case "CANCELLED":
      return "bg-red-100 text-red-700";

    case "PLANNED":
    case "PENDING":
      return "bg-yellow-100 text-yellow-700";

    default:
      return "bg-slate-100 text-slate-700";
  }
}

function Monitoring() {
  const [shipments, setShipments] = useState([]);
  const [selectedShipmentId, setSelectedShipmentId] = useState("");

  const [location, setLocation] = useState("");
  const [latitude, setLatitude] = useState("");
  const [longitude, setLongitude] = useState("");
  const [notes, setNotes] = useState("");

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [lastUpdated, setLastUpdated] = useState(null);

  const loadActiveShipments = useCallback(async () => {
    try {
      setLoading(true);
      setError("");

      const response = await api.get("/monitoring/active");

      console.log("Monitoring API response:", response.data);

      const data = Array.isArray(response.data)
        ? response.data
        : [];

      setShipments(data);
      setLastUpdated(new Date());

      // Clear selected shipment if it is no longer active.
      const stillExists = data.some(
        (shipment) =>
          String(getShipmentId(shipment)) ===
          String(selectedShipmentId)
      );

      if (!stillExists) {
        setSelectedShipmentId("");
        setLocation("");
        setLatitude("");
        setLongitude("");
        setNotes("");
      }
    } catch (err) {
      console.error("Could not load active shipments:", err);

      setError(
        err.response?.data?.message ||
          "Could not load active shipments. Check whether the backend is running."
      );

      setShipments([]);
    } finally {
      setLoading(false);
    }
  }, [selectedShipmentId]);

  useEffect(() => {
    loadActiveShipments();
  }, [loadActiveShipments]);

  const selectedShipment = useMemo(() => {
    return shipments.find(
      (shipment) =>
        String(getShipmentId(shipment)) ===
        String(selectedShipmentId)
    );
  }, [shipments, selectedShipmentId]);

  function handleShipmentChange(event) {
    const value = event.target.value;

    setSelectedShipmentId(value);
    setError("");
    setSuccess("");

    const shipment = shipments.find(
      (item) =>
        String(getShipmentId(item)) === String(value)
    );

    if (!shipment) {
      setLocation("");
      setLatitude("");
      setLongitude("");
      setNotes("");
      return;
    }

    setLocation(getLocation(shipment));
    setLatitude(getLatitude(shipment));
    setLongitude(getLongitude(shipment));
    setNotes("");
  }

  async function handleSaveLocation(event) {
    event.preventDefault();

    setError("");
    setSuccess("");

    if (!selectedShipmentId) {
      setError("Please select an active shipment.");
      return;
    }

    if (!selectedShipment) {
      setError("Selected shipment was not found. Refresh the page.");
      return;
    }

    const routeId = getRouteId(selectedShipment);

    console.log("Selected shipment:", selectedShipment);
    console.log("Route ID:", routeId);

    if (!routeId) {
      setError(
        "This shipment has no route assigned. Assign an existing route first."
      );
      return;
    }

    if (!location.trim()) {
      setError("Please enter a location.");
      return;
    }

    if (latitude === "" || longitude === "") {
      setError("Please enter latitude and longitude.");
      return;
    }

    const latitudeNumber = Number(latitude);
    const longitudeNumber = Number(longitude);

    if (
      !Number.isFinite(latitudeNumber) ||
      latitudeNumber < -90 ||
      latitudeNumber > 90
    ) {
      setError("Latitude must be between -90 and 90.");
      return;
    }

    if (
      !Number.isFinite(longitudeNumber) ||
      longitudeNumber < -180 ||
      longitudeNumber > 180
    ) {
      setError("Longitude must be between -180 and 180.");
      return;
    }

    const payload = {
      location: location.trim(),
      latitude: latitudeNumber,
      longitude: longitudeNumber,
      notes: notes.trim(),
    };

    try {
      setSaving(true);

      /*
       * API base URL:
       * http://localhost:8080/api
       *
       * Final request:
       * POST http://localhost:8080/api/route/{routeId}/location
       */
      await api.post(`/route/${routeId}/location`, payload);

      setSuccess(
        `Location updated successfully for route ${routeId}.`
      );

      setNotes("");

      await loadActiveShipments();
    } catch (err) {
      console.error("Could not save location:", err);

      setError(
        err.response?.data?.message ||
          "Could not save location. Check the backend endpoint and route ID."
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="min-h-screen bg-slate-100 px-4 py-8">
      <div className="mx-auto max-w-7xl">
        <div className="mb-6 flex flex-col justify-between gap-4 md:flex-row md:items-center">
          <div>
            <h1 className="text-3xl font-semibold text-slate-900">
              Live monitoring
            </h1>

            <p className="mt-2 text-slate-500">
              Driver positions arrive when they are recorded.
              {lastUpdated && (
                <>
                  {" "}
                  Updated {lastUpdated.toLocaleTimeString()}.
                </>
              )}
            </p>
          </div>

          <button
            type="button"
            onClick={loadActiveShipments}
            disabled={loading}
            className="rounded-lg border border-slate-300 bg-white px-5 py-3 font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? "Refreshing..." : "Refresh now"}
          </button>
        </div>

        {error && (
          <div className="mb-5 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-red-700">
            {error}
          </div>
        )}

        {success && (
          <div className="mb-5 rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-green-700">
            {success}
          </div>
        )}

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="mb-6">
            <h2 className="text-xl font-semibold text-slate-800">
              Location update
            </h2>

            <p className="mt-1 text-slate-500">
              Record the latest location for an active shipment.
            </p>
          </div>

          <form
            onSubmit={handleSaveLocation}
            className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-5"
          >
            <div>
              <label className="mb-2 block font-semibold text-slate-700">
                Shipment
              </label>

              <select
                value={selectedShipmentId}
                onChange={handleShipmentChange}
                disabled={loading || shipments.length === 0}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-3 disabled:cursor-not-allowed disabled:bg-slate-100"
              >
                <option value="">
                  {loading
                    ? "Loading shipments..."
                    : shipments.length === 0
                    ? "No active shipments"
                    : "Select shipment"}
                </option>

                {shipments.map((shipment) => {
                  const shipmentId = getShipmentId(shipment);

                  return (
                    <option
                      key={shipmentId}
                      value={shipmentId}
                    >
                      Shipment {shipmentId}
                    </option>
                  );
                })}
              </select>
            </div>

            <div>
              <label className="mb-2 block font-semibold text-slate-700">
                Location
              </label>

              <input
                type="text"
                value={location}
                onChange={(event) =>
                  setLocation(event.target.value)
                }
                placeholder="Enter location"
                className="w-full rounded-lg border border-slate-300 px-3 py-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
              />
            </div>

            <div>
              <label className="mb-2 block font-semibold text-slate-700">
                Latitude
              </label>

              <input
                type="number"
                step="any"
                value={latitude}
                onChange={(event) =>
                  setLatitude(event.target.value)
                }
                placeholder="19.0760"
                className="w-full rounded-lg border border-slate-300 px-3 py-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
              />
            </div>

            <div>
              <label className="mb-2 block font-semibold text-slate-700">
                Longitude
              </label>

              <input
                type="number"
                step="any"
                value={longitude}
                onChange={(event) =>
                  setLongitude(event.target.value)
                }
                placeholder="72.8777"
                className="w-full rounded-lg border border-slate-300 px-3 py-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
              />
            </div>

            <div>
              <label className="mb-2 block font-semibold text-slate-700">
                Notes
              </label>

              <input
                type="text"
                value={notes}
                onChange={(event) =>
                  setNotes(event.target.value)
                }
                placeholder="Optional update"
                className="w-full rounded-lg border border-slate-300 px-3 py-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
              />
            </div>

            <div className="md:col-span-2 lg:col-span-5">
              <button
                type="submit"
                disabled={saving || !selectedShipment}
                className="rounded-lg bg-blue-600 px-6 py-3 font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {saving ? "Saving..." : "Save location update"}
              </button>
            </div>
          </form>
        </section>

        <section className="mt-8 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="bg-slate-50">
                <tr className="border-b border-slate-200 text-left text-sm text-slate-600">
                  <th className="px-5 py-4 font-semibold">
                    Route ID
                  </th>

                  <th className="px-5 py-4 font-semibold">
                    Shipment ID
                  </th>

                  <th className="px-5 py-4 font-semibold">
                    Status
                  </th>

                  <th className="px-5 py-4 font-semibold">
                    Last location
                  </th>

                  <th className="px-5 py-4 font-semibold">
                    Latitude
                  </th>

                  <th className="px-5 py-4 font-semibold">
                    Longitude
                  </th>
                </tr>
              </thead>

              <tbody>
                {loading ? (
                  <tr>
                    <td
                      colSpan="6"
                      className="px-5 py-8 text-center text-slate-500"
                    >
                      Loading active shipments...
                    </td>
                  </tr>
                ) : shipments.length === 0 ? (
                  <tr>
                    <td
                      colSpan="6"
                      className="px-5 py-8 text-center text-slate-500"
                    >
                      No active shipments found.
                    </td>
                  </tr>
                ) : (
                  shipments.map((shipment) => {
                    const shipmentId = getShipmentId(shipment);
                    const routeId = getRouteId(shipment);
                    const status = getStatus(shipment);

                    return (
                      <tr
                        key={shipmentId}
                        className="border-b border-slate-100 last:border-b-0 hover:bg-slate-50"
                      >
                        <td className="px-5 py-4 text-slate-700">
                          {formatValue(routeId)}
                        </td>

                        <td className="px-5 py-4 text-slate-700">
                          {formatValue(shipmentId)}
                        </td>

                        <td className="px-5 py-4">
                          <span
                            className={`rounded-full px-3 py-1 text-xs font-semibold ${getStatusClass(
                              status
                            )}`}
                          >
                            {status}
                          </span>
                        </td>

                        <td className="px-5 py-4 text-slate-700">
                          {formatValue(getLocation(shipment))}
                        </td>

                        <td className="px-5 py-4 text-slate-700">
                          {formatValue(getLatitude(shipment))}
                        </td>

                        <td className="px-5 py-4 text-slate-700">
                          {formatValue(getLongitude(shipment))}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
}

export default Monitoring;