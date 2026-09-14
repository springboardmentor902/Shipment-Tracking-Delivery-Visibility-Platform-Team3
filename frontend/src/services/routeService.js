import api from "./api";

export const ROUTE_LEG_STATUSES = [
  "PLANNED",
  "ACTIVE",
  "COMPLETED",
  "SKIPPED",
];

export const CAN_MANAGE_ROUTE_ROLES = [
  "LOGISTICS_OPERATOR",
  "ADMINISTRATOR",
];

export const routeService = {
  /**
   * Get route by shipment ID
   */
  listByShipment: (shipmentId) => {
    if (shipmentId === undefined || shipmentId === null) {
      throw new Error("Shipment ID is required.");
    }

    return api
      .get(`/routes/${encodeURIComponent(String(shipmentId))}`)
      .then((response) => response.data);
  },

  /**
   * Get one route
   */
  getLeg: (routeId) => {
    if (routeId === undefined || routeId === null) {
      throw new Error("Route ID is required.");
    }

    return api
      .get(`/routes/leg/${encodeURIComponent(String(routeId))}`)
      .then((response) => response.data);
  },

  /**
   * Create route
   */
  create: (payload) => {
    return api
      .post("/routes", payload)
      .then((response) => response.data);
  },

  /**
   * Update route location
   *
   * Backend endpoint:
   * POST /api/route/{id}/location
   */
  updateLocation: (routeId, payload) => {
    if (
      routeId === undefined ||
      routeId === null ||
      routeId === "" ||
      typeof routeId === "object"
    ) {
      throw new Error(
        "Invalid route ID. Pass a route ID number, for example updateLocation(5, payload)."
      );
    }

    console.log("Updating route location:", {
      routeId,
      payload,
    });

    return api
      .post(
        `/route/${encodeURIComponent(String(routeId))}/location`,
        payload
      )
      .then((response) => response.data);
  },

  /**
   * Refresh route using Google Maps/OpenStreetMap
   */
  refreshFromMaps: (routeId) => {
    if (routeId === undefined || routeId === null) {
      throw new Error("Route ID is required.");
    }

    return api
      .post(
        `/routes/${encodeURIComponent(String(routeId))}/refresh`
      )
      .then((response) => response.data);
  },

  /**
   * Update route status
   */
  updateStatus: (routeId, status) => {
    return api
      .patch(
        `/routes/${encodeURIComponent(String(routeId))}/status`,
        null,
        {
          params: {
            status,
          },
        }
      )
      .then((response) => response.data);
  },

  /**
   * Assign driver
   */
  assignDriver: (routeId, driverId) => {
    return api
      .patch(
        `/routes/${encodeURIComponent(String(routeId))}/driver`,
        null,
        {
          params: {
            driverId,
          },
        }
      )
      .then((response) => response.data);
  },

  /**
   * Get route history
   */
  getHistory: (shipmentId) => {
    return api
      .get(
        `/routes/${encodeURIComponent(String(shipmentId))}/history`
      )
      .then((response) => response.data);
  },
};

export const METRICS_SOURCE_LABELS = {
  LIVE_MAPS: "Google Maps",
  STRAIGHT_LINE: "Straight-line estimate",
  MANUAL: "Entered manually",
};

export function formatDuration(minutes) {
  if (
    minutes === null ||
    minutes === undefined ||
    minutes === ""
  ) {
    return "—";
  }

  const total = Number(minutes);

  if (!Number.isFinite(total)) {
    return "—";
  }

  const hours = Math.floor(total / 60);
  const rest = total % 60;

  if (hours === 0) {
    return `${rest} min`;
  }

  return rest
    ? `${hours} h ${rest} min`
    : `${hours} h`;
}