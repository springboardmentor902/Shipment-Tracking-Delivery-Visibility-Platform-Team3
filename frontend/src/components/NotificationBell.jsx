import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  fetchNotifications,
  fetchUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
  relativeTime,
} from '../services/notificationService'

const POLL_INTERVAL_MS = 60_000
const PREVIEW_SIZE = 8

const SEVERITY_DOT = {
  CRITICAL: 'bg-red-500',
  WARNING: 'bg-amber-500',
  INFO: 'bg-brand-500',
}

/**
 * Header bell with an unread badge and a short preview list.
 *
 * The count is polled rather than pushed: alerts are not time-critical to the
 * minute, and a poll keeps this independent of the tracking socket.
 */
export default function NotificationBell() {
  const [open, setOpen] = useState(false)
  const [unread, setUnread] = useState(0)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const containerRef = useRef(null)
  const navigate = useNavigate()

  const loadCount = useCallback(async () => {
    try {
      setUnread(await fetchUnreadCount())
    } catch {
      // a failed poll is not worth surfacing; the next one may succeed
    }
  }, [])

  useEffect(() => {
    loadCount()
    const timer = setInterval(loadCount, POLL_INTERVAL_MS)
    return () => clearInterval(timer)
  }, [loadCount])

  // close on outside click and on Escape
  useEffect(() => {
    if (!open) return undefined

    function onPointerDown(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setOpen(false)
      }
    }
    function onKeyDown(event) {
      if (event.key === 'Escape') setOpen(false)
    }

    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  async function togglePanel() {
    const next = !open
    setOpen(next)
    if (!next) return

    setLoading(true)
    try {
      setItems(await fetchNotifications({ limit: PREVIEW_SIZE }))
      await loadCount()
    } catch {
      setItems([])
    } finally {
      setLoading(false)
    }
  }

  async function openNotification(notification) {
    setOpen(false)
    if (!notification.read) {
      setUnread((current) => Math.max(0, current - 1))
      setItems((current) =>
        current.map((item) => (item.id === notification.id ? { ...item, read: true } : item)),
      )
      try {
        await markNotificationRead(notification.id)
      } catch {
        // the row stays unread server-side; the next poll corrects the badge
        loadCount()
      }
    }
    if (notification.shipmentId) {
      navigate(`/shipments/${notification.shipmentId}`)
    }
  }

  async function clearAll() {
    setUnread(0)
    setItems((current) => current.map((item) => ({ ...item, read: true })))
    try {
      await markAllNotificationsRead()
    } catch {
      loadCount()
    }
  }

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={togglePanel}
        aria-label={unread > 0 ? `Notifications, ${unread} unread` : 'Notifications'}
        aria-expanded={open}
        className="relative rounded-lg border border-slate-300 px-2.5 py-1.5 text-slate-600 transition hover:bg-slate-50 hover:text-brand-700"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="h-5 w-5"
          aria-hidden="true"
        >
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>

        {unread > 0 && (
          <span className="absolute -right-1 -top-1 min-w-5 rounded-full bg-red-600 px-1 text-[11px] font-semibold leading-5 text-white">
            {unread > 99 ? '99+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-50 mt-2 w-80 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg sm:w-96">
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
            <span className="text-sm font-semibold text-slate-900">Notifications</span>
            {unread > 0 && (
              <button
                type="button"
                onClick={clearAll}
                className="text-xs font-medium text-brand-700 hover:underline"
              >
                Mark all read
              </button>
            )}
          </div>

          <div className="max-h-96 overflow-y-auto">
            {loading && <p className="px-4 py-6 text-sm text-slate-500">Loading…</p>}

            {!loading && items.length === 0 && (
              <p className="px-4 py-6 text-sm text-slate-500">
                Nothing yet. Status changes and delay warnings will show up here.
              </p>
            )}

            {!loading &&
              items.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => openNotification(item)}
                  className={`flex w-full gap-3 border-b border-slate-50 px-4 py-3 text-left transition hover:bg-slate-50 ${
                    item.read ? 'bg-white' : 'bg-brand-50/40'
                  }`}
                >
                  <span
                    className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${
                      SEVERITY_DOT[item.severity] || SEVERITY_DOT.INFO
                    } ${item.read ? 'opacity-30' : ''}`}
                    aria-hidden="true"
                  />
                  <span className="min-w-0 flex-1">
                    <span
                      className={`block truncate text-sm ${
                        item.read ? 'text-slate-600' : 'font-semibold text-slate-900'
                      }`}
                    >
                      {item.title}
                    </span>
                    <span className="mt-0.5 block line-clamp-2 text-xs text-slate-500">
                      {item.message}
                    </span>
                    <span className="mt-1 block text-[11px] text-slate-400">
                      {relativeTime(item.createdAt)}
                      {item.smsSent ? ' · SMS sent' : item.emailSent ? ' · emailed' : ''}
                    </span>
                  </span>
                </button>
              ))}
          </div>

          <div className="border-t border-slate-100 px-4 py-2.5 text-center">
            <Link
              to="/notifications"
              onClick={() => setOpen(false)}
              className="text-sm font-medium text-brand-700 hover:underline"
            >
              See all notifications
            </Link>
          </div>
        </div>
      )}
    </div>
  )
}
