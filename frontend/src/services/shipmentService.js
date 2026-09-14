import api from "./api";

// --------------------------------------------------
// Shipment statuses
// --------------------------------------------------

export const SHIPMENT_STATUSES = [
  "CREATED",
  "PICKED_UP",
  "IN_TRANSIT",
  "OUT_FOR_DELIVERY",
  "DELIVERED",
  "FAILED_DELIVERY",
  "CANCELLED",
];

// --------------------------------------------------
// Allowed status transitions
// --------------------------------------------------

export const ALLOWED_STATUS_TRANSITIONS = {
  CREATED: ["PICKED_UP", "CANCELLED"],

  PICKED_UP: [
    "IN_TRANSIT",
    "FAILED_DELIVERY",
    "CANCELLED",
  ],

  IN_TRANSIT: [
    "OUT_FOR_DELIVERY",
    "FAILED_DELIVERY",
    "CANCELLED",
  ],

  OUT_FOR_DELIVERY: [
    "DELIVERED",
    "FAILED_DELIVERY",
  ],

  FAILED_DELIVERY: [
    "OUT_FOR_DELIVERY",
    "CANCELLED",
  ],

  DELIVERED: [],

  CANCELLED: [],
};

// --------------------------------------------------
// Role permissions
// --------------------------------------------------

export const CAN_CREATE_ROLES = [
  "CUSTOMER",
  "BUSINESS_CLIENT",
  "LOGISTICS_OPERATOR",
];

export const CAN_CHANGE_STATUS_ROLES = [
  "LOGISTICS_OPERATOR",
  "ADMINISTRATOR",
];

// --------------------------------------------------
// Shipment service
// --------------------------------------------------

export const shipmentService = {
  // Get shipments with pagination and optional status
  list: ({
    status,
    page = 0,
    size = 10,
  } = {}) =>
    api
      .get("/shipments", {
        params: {
          status: status || undefined,
          page,
          size,
        },
      })
      .then((response) => response.data),

  // Get shipment by database ID
  getById: (id) =>
    api
      .get(`/shipments/${id}`)
      .then((response) => response.data),

  // Get shipment by tracking number
  getByTracking: (trackingNumber) =>
    api
      .get(
        `/shipments/${encodeURIComponent(trackingNumber)}`
      )
      .then((response) => response.data),

  // Create shipment
  create: (payload) =>
    api
      .post("/shipments", payload)
      .then((response) => response.data),

  // Update shipment details
  update: (id, payload) =>
    api
      .put(`/shipments/${id}`, payload)
      .then((response) => response.data),

  // Update shipment status
  updateStatus: (id, status, note = "") =>
    api
      .patch(`/shipments/${id}/status`, {
        status,
        notes: note,
      })
      .then((response) => response.data),

  // Get tracking timeline
  getTrackingEvents: (shipmentId) =>
    api
      .get(`/shipments/${shipmentId}/tracking`)
      .then((response) => response.data),

  // Add a checkpoint to shipment tracking timeline
  addTrackingEvent: (shipmentId, payload) =>
    api
      .post(`/shipments/${shipmentId}/tracking`, payload)
      .then((response) => response.data),

  // Get routes belonging to a shipment
  getRoutes: (shipmentId) =>
    api
      .get(`/shipments/${shipmentId}/routes`)
      .then((response) => response.data),

  // Assign operator to shipment
  assignOperator: (shipmentId, operatorId) =>
    api
      .patch(
        `/shipments/${shipmentId}/operator`,
        null,
        {
          params: {
            operatorId,
          },
        }
      )
      .then((response) => response.data),

  // Get active shipments for monitoring
  getActiveMonitoring: () =>
    api
      .get("/monitoring/active")
      .then((response) => response.data),

  // Get admin users
  getAdminUsers: () =>
    api
      .get("/admin/users")
      .then((response) => response.data),

  // Cancel shipment
  cancel: (shipmentId, reason = "") =>
    api
      .delete(`/shipments/${shipmentId}`, {
        data: {
          reason,
        },
      })
      .then((response) => response.data),
};