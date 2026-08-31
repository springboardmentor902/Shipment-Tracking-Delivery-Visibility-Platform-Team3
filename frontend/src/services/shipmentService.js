import api from './api'

export const SHIPMENT_STATUSES = [
  'CREATED',
  'PICKED_UP',
  'IN_TRANSIT',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
  'FAILED_DELIVERY',
  'CANCELLED',
]

// Keep the UI aligned with the backend's closed shipment lifecycle.
export const ALLOWED_STATUS_TRANSITIONS = {
  CREATED: ['PICKED_UP', 'CANCELLED'],
  PICKED_UP: ['IN_TRANSIT', 'FAILED_DELIVERY', 'CANCELLED'],
  IN_TRANSIT: ['OUT_FOR_DELIVERY', 'FAILED_DELIVERY', 'CANCELLED'],
  OUT_FOR_DELIVERY: ['DELIVERED', 'FAILED_DELIVERY'],
  FAILED_DELIVERY: ['OUT_FOR_DELIVERY', 'CANCELLED'],
  DELIVERED: [],
  CANCELLED: [],
}

// Customers book their own shipments too (Milestone 1).
export const CAN_CREATE_ROLES = ['CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR']
export const CAN_CHANGE_STATUS_ROLES = ['LOGISTICS_OPERATOR', 'ADMINISTRATOR']

export const shipmentService = {
  /** Spring returns a Page object: { content, totalPages, totalElements, number, size } */
  list: ({ status, page = 0, size = 10 } = {}) =>
    api
      .get('/shipments', { params: { status: status || undefined, page, size } })
      .then((res) => res.data),

  getById: (id) => api.get(`/shipments/${id}`).then((res) => res.data),

  getByTracking: (trackingNumber) =>
    api.get(`/tracking/${encodeURIComponent(trackingNumber)}`).then((res) => res.data),

  create: (payload) => api.post('/shipments', payload).then((res) => res.data),

  update: (id, payload) => api.put(`/shipments/${id}`, payload).then((res) => res.data),

  updateStatus: (id, status, note) =>
    api.patch(`/shipments/${id}/status`, { status, notes: note }).then((res) => res.data),

  getTrackingEvents: (id) => api.get(`/shipments/${id}/tracking`).then((res) => res.data),

  updateLocation: (payload) => api.post('/tracking/location', payload).then((res) => res.data),

  /** Add a checkpoint to the timeline by hand, e.g. "reached Vijayawada hub". */
  addTrackingEvent: (payload) => api.post('/tracking/events', payload).then((res) => res.data),

  /** All route legs of a shipment, in travel order. See also routeService. */
  getRoutes: (id) => api.get(`/routes/${id}`).then((res) => res.data),

  assignOperator: (id, operatorId) =>
    api.patch(`/shipments/${id}/operator`, { operatorId }).then((res) => res.data),

  getActiveMonitoring: () => api.get('/monitoring/active').then((res) => res.data),

  getAdminUsers: () => api.get('/admin/users').then((res) => res.data),

  // DELETE with a body needs the `data` key in axios.
  cancel: (id, reason) =>
    api.delete(`/shipments/${id}`, { data: { reason } }).then((res) => res.data),
}
