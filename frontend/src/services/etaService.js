import api from './api'

/** Delivery forecast and delay-risk APIs. */
export const etaService = {
  /** Stored forecast for one shipment; calculated on demand if missing. */
  getForShipment: (shipmentId) => api.get(`/eta/shipments/${shipmentId}`).then((res) => res.data),

  /** Operator, support and admin only. */
  recalculate: (shipmentId) =>
    api.post(`/eta/shipments/${shipmentId}/recalculate`).then((res) => res.data),

  /** Shipments likely to miss their promise, scoped to what the caller may see. */
  listAtRisk: (minScore = 50) =>
    api.get('/eta/at-risk', { params: { minScore } }).then((res) => res.data),
}

export const RISK_STYLES = {
  LOW: { label: 'On track', badge: 'bg-emerald-50 text-emerald-700', bar: 'bg-emerald-500' },
  MEDIUM: { label: 'Watch', badge: 'bg-amber-50 text-amber-700', bar: 'bg-amber-500' },
  HIGH: { label: 'At risk', badge: 'bg-orange-50 text-orange-700', bar: 'bg-orange-500' },
  CRITICAL: { label: 'Critical', badge: 'bg-red-50 text-red-700', bar: 'bg-red-500' },
}

export const ETA_SOURCE_LABELS = {
  ROUTE_METRICS: 'Based on planned route legs',
  STATUS_HEURISTIC: 'Estimated from status only',
  COMPLETED: 'Journey finished',
}

export function riskStyle(level) {
  return RISK_STYLES[level] || RISK_STYLES.LOW
}

/** "2 days 3 hours late" / "on time" — the sign carries the meaning. */
export function formatDelay(minutes) {
  if (minutes === null || minutes === undefined) return 'No promised date'
  if (minutes <= 0) return 'Within the promised window'

  const days = Math.floor(minutes / (24 * 60))
  const hours = Math.floor((minutes % (24 * 60)) / 60)
  const rest = minutes % 60

  const parts = []
  if (days) parts.push(`${days} day${days === 1 ? '' : 's'}`)
  if (hours) parts.push(`${hours} hour${hours === 1 ? '' : 's'}`)
  if (!days && !hours) parts.push(`${rest} min`)
  return `${parts.join(' ')} late`
}

export function formatEta(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString([], {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}
