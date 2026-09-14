import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { extractErrorMessage } from '../services/api'
import {
  fetchNotificationPreferences,
  fetchNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  relativeTime,
  updateNotificationPreferences,
  SEVERITY_STYLES,
} from '../services/notificationService'

const RISK_LEVELS = [
  { value: 'LOW', label: 'Any risk' },
  { value: 'MEDIUM', label: 'Medium and above' },
  { value: 'HIGH', label: 'High and above' },
  { value: 'CRITICAL', label: 'Critical only' },
]

const CHANNELS = [
  { key: 'inAppEnabled', label: 'In-app', hint: 'Shown in the bell menu and on this page.' },
  { key: 'emailEnabled', label: 'Email', hint: 'Sent to your account email address.' },
  {
    key: 'smsEnabled',
    label: 'SMS',
    hint: 'Delay and delivery alerts only, to the phone number on your profile.',
  },
]

const TOPICS = [
  { key: 'notifyStatusChange', label: 'Status changes', hint: 'Picked up, in transit, out for delivery.' },
  { key: 'notifyDelayRisk', label: 'Delay warnings', hint: 'Raised when a shipment may miss its promise.' },
  { key: 'notifyDelivery', label: 'Delivery and proof', hint: 'Delivered, failed attempts, verified proof.' },
]

export default function Notifications() {
  const [items, setItems] = useState([])
  const [preferences, setPreferences] = useState(null)
  const [unreadOnly, setUnreadOnly] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [list, prefs] = await Promise.all([
        fetchNotifications({ unreadOnly, limit: 50 }),
        fetchNotificationPreferences(),
      ])
      setItems(list)
      setPreferences(prefs)
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not load your notifications.'))
    } finally {
      setLoading(false)
    }
  }, [unreadOnly])

  useEffect(() => {
    load()
  }, [load])

  async function toggle(key) {
    if (!preferences) return
    const next = { [key]: !preferences[key] }

    setSaving(true)
    setError('')
    setNotice('')
    try {
      setPreferences(await updateNotificationPreferences(next))
      setNotice('Preferences saved.')
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not save that preference.'))
    } finally {
      setSaving(false)
    }
  }

  async function changeRiskFloor(value) {
    setSaving(true)
    setError('')
    setNotice('')
    try {
      setPreferences(await updateNotificationPreferences({ minRiskLevel: value }))
      setNotice('Preferences saved.')
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not save that preference.'))
    } finally {
      setSaving(false)
    }
  }

  async function readOne(notification) {
    if (notification.read) return
    setItems((current) =>
      current.map((item) => (item.id === notification.id ? { ...item, read: true } : item)),
    )
    try {
      await markNotificationRead(notification.id)
    } catch {
      load()
    }
  }

  async function readAll() {
    setItems((current) => current.map((item) => ({ ...item, read: true })))
    try {
      await markAllNotificationsRead()
      if (unreadOnly) load()
    } catch {
      load()
    }
  }

  const unreadCount = items.filter((item) => !item.read).length

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Notifications</h1>
          <p className="mt-1 text-sm text-slate-500">
            Delivery updates and delay warnings for the shipments you are involved in.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setUnreadOnly((value) => !value)}
            className={`rounded-lg border px-3 py-1.5 text-sm font-medium transition ${
              unreadOnly
                ? 'border-brand-600 bg-brand-50 text-brand-700'
                : 'border-slate-300 text-slate-700 hover:bg-slate-50'
            }`}
          >
            {unreadOnly ? 'Showing unread' : 'Show unread only'}
          </button>
          <button
            type="button"
            onClick={readAll}
            disabled={unreadCount === 0}
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Mark all read
          </button>
        </div>
      </div>

      {error && (
        <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}
      {notice && !error && (
        <p className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {notice}
        </p>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        {/* ---- the inbox ---- */}
        <section className="lg:col-span-2">
          <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
            {loading && <p className="px-5 py-8 text-sm text-slate-500">Loading notifications…</p>}

            {!loading && items.length === 0 && (
              <p className="px-5 py-8 text-sm text-slate-500">
                {unreadOnly
                  ? 'Nothing unread. Everything here has been read.'
                  : 'No notifications yet. Status changes and delay warnings will appear here.'}
              </p>
            )}

            {!loading &&
              items.map((item) => (
                <article
                  key={item.id}
                  className={`border-b border-slate-100 px-5 py-4 last:border-b-0 ${
                    item.read ? 'bg-white' : 'bg-brand-50/40'
                  }`}
                >
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <h2
                      className={`text-sm ${
                        item.read ? 'text-slate-700' : 'font-semibold text-slate-900'
                      }`}
                    >
                      {item.title}
                    </h2>
                    <span
                      className={`rounded-full border px-2 py-0.5 text-[11px] font-medium ${
                        SEVERITY_STYLES[item.severity] || SEVERITY_STYLES.INFO
                      }`}
                    >
                      {(item.severity || 'INFO').toLowerCase()}
                    </span>
                  </div>

                  <p className="mt-1 text-sm text-slate-600">{item.message}</p>

                  <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-slate-400">
                    <span>{relativeTime(item.createdAt)}</span>
                    {item.emailSent && <span>emailed</span>}
                    {item.smsSent && <span>SMS sent</span>}
                    {item.shipmentId && (
                      <Link
                        to={`/shipments/${item.shipmentId}`}
                        onClick={() => readOne(item)}
                        className="font-medium text-brand-700 hover:underline"
                      >
                        Open {item.trackingNumber || 'shipment'}
                      </Link>
                    )}
                    {!item.read && (
                      <button
                        type="button"
                        onClick={() => readOne(item)}
                        className="font-medium text-slate-500 hover:text-brand-700 hover:underline"
                      >
                        Mark read
                      </button>
                    )}
                  </div>
                </article>
              ))}
          </div>
        </section>

        {/* ---- settings ---- */}
        <section className="space-y-4">
          <div className="rounded-xl border border-slate-200 bg-white p-5">
            <h2 className="text-sm font-semibold text-slate-900">Channels</h2>
            <p className="mt-1 text-xs text-slate-500">
              Where alerts are delivered. SMS is used for delay and delivery alerts only.
            </p>

            <div className="mt-4 space-y-3">
              {CHANNELS.map((channel) => {
                const unavailable =
                  (channel.key === 'emailEnabled' && preferences && !preferences.emailChannelAvailable) ||
                  (channel.key === 'smsEnabled' && preferences && !preferences.smsChannelAvailable)

                return (
                  <label key={channel.key} className="flex gap-3">
                    <input
                      type="checkbox"
                      checked={Boolean(preferences?.[channel.key])}
                      onChange={() => toggle(channel.key)}
                      disabled={!preferences || saving}
                      className="mt-0.5 h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
                    />
                    <span className="min-w-0">
                      <span className="block text-sm font-medium text-slate-800">{channel.label}</span>
                      <span className="block text-xs text-slate-500">{channel.hint}</span>
                      {unavailable && (
                        <span className="mt-0.5 block text-xs text-amber-600">
                          Not configured on the server yet, so nothing will be sent.
                        </span>
                      )}
                      {channel.key === 'smsEnabled' && preferences && !preferences.phone && (
                        <span className="mt-0.5 block text-xs text-amber-600">
                          Add a phone number to your profile first.
                        </span>
                      )}
                    </span>
                  </label>
                )
              })}
            </div>
          </div>

          <div className="rounded-xl border border-slate-200 bg-white p-5">
            <h2 className="text-sm font-semibold text-slate-900">What to tell me about</h2>

            <div className="mt-4 space-y-3">
              {TOPICS.map((topic) => (
                <label key={topic.key} className="flex gap-3">
                  <input
                    type="checkbox"
                    checked={Boolean(preferences?.[topic.key])}
                    onChange={() => toggle(topic.key)}
                    disabled={!preferences || saving}
                    className="mt-0.5 h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
                  />
                  <span className="min-w-0">
                    <span className="block text-sm font-medium text-slate-800">{topic.label}</span>
                    <span className="block text-xs text-slate-500">{topic.hint}</span>
                  </span>
                </label>
              ))}
            </div>

            <div className="mt-5">
              <label
                htmlFor="minRiskLevel"
                className="block text-sm font-medium text-slate-800"
              >
                Warn me from this delay risk upwards
              </label>
              <select
                id="minRiskLevel"
                value={preferences?.minRiskLevel || 'HIGH'}
                onChange={(event) => changeRiskFloor(event.target.value)}
                disabled={!preferences || saving}
                className="mt-2 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
              >
                {RISK_LEVELS.map((level) => (
                  <option key={level.value} value={level.value}>
                    {level.label}
                  </option>
                ))}
              </select>
              <p className="mt-1.5 text-xs text-slate-500">
                Lower risk shipments still appear on the{' '}
                <Link to="/delays" className="font-medium text-brand-700 hover:underline">
                  delays page
                </Link>
                .
              </p>
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
