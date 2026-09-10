import api from './api'

export const ROUTE_LEG_STATUSES = ['PLANNED', 'ACTIVE', 'COMPLETED', 'SKIPPED']

export const CAN_MANAGE_ROUTE_ROLES = ['LOGISTICS_OPERATOR', 'ADMINISTRATOR']

/** Multi-leg route APIs. A shipment can have many legs, ordered by legNumber. */
export const routeService = {
  /** All legs of a shipment, in travel order. */
  listByShipment: (shipmentId) => api.get(`/routes/${shipmentId}`).then((res) => res.data),

  getLeg: (routeId) => api.get(`/routes/leg/${routeId}`).then((res) => res.data),

  create: (payload) => api.post('/routes', payload).then((res) => res.data),

  update: (routeId, payload) => api.put(`/routes/${routeId}`, payload).then((res) => res.data),

  /** Recalculate distance, duration and traffic from Google Maps. */
  refreshFromMaps: (routeId) => api.post(`/routes/${routeId}/refresh`).then((res) => res.data),
}

/** Human labels for the metricsSource the backend reports. */
export const METRICS_SOURCE_LABELS = {
  LIVE_MAPS: 'Google Maps',
  STRAIGHT_LINE: 'Straight-line estimate',
  MANUAL: 'Entered manually',
}

export function formatDuration(minutes) {
  if (minutes === null || minutes === undefined || minutes === '') return '—'
  const total = Number(minutes)
  if (!Number.isFinite(total)) return '—'
  const hours = Math.floor(total / 60)
  const rest = total % 60
  if (!hours) return `${rest} min`
  return rest ? `${hours} h ${rest} min` : `${hours} h`
}
