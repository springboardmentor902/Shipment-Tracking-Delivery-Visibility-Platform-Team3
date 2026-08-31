import api from './api'

/**
 * Alert inbox and per-user alert settings.
 *
 * Every endpoint works on the signed-in user's own notifications: there is no
 * user id to pass, and the backend takes the recipient from the token.
 */

export async function fetchNotifications({ unreadOnly = false, limit = 20 } = {}) {
  const { data } = await api.get('/notifications', { params: { unreadOnly, limit } })
  return Array.isArray(data) ? data : []
}

export async function fetchUnreadCount() {
  const { data } = await api.get('/notifications/unread-count')
  return Number(data?.unread ?? 0)
}

export async function markNotificationRead(id) {
  const { data } = await api.post(`/notifications/${id}/read`)
  return data
}

export async function markAllNotificationsRead() {
  const { data } = await api.post('/notifications/read-all')
  return Number(data?.updated ?? 0)
}

export async function fetchNotificationPreferences() {
  const { data } = await api.get('/notifications/preferences')
  return data
}

export async function updateNotificationPreferences(patch) {
  const { data } = await api.put('/notifications/preferences', patch)
  return data
}

/** "3 min ago" style label; alerts are mostly minutes or hours old. */
export function relativeTime(isoString) {
  if (!isoString) return ''
  const then = new Date(isoString).getTime()
  if (Number.isNaN(then)) return ''

  const minutes = Math.round((Date.now() - then) / 60000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes} min ago`

  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours} ${hours === 1 ? 'hour' : 'hours'} ago`

  const days = Math.round(hours / 24)
  if (days < 7) return `${days} ${days === 1 ? 'day' : 'days'} ago`

  return new Date(isoString).toLocaleDateString()
}

export const SEVERITY_STYLES = {
  CRITICAL: 'bg-red-50 text-red-700 border-red-200',
  WARNING: 'bg-amber-50 text-amber-700 border-amber-200',
  INFO: 'bg-slate-50 text-slate-600 border-slate-200',
}
