import StatusBadge from './StatusBadge'

function formatDateTime(value) {
  if (!value) return 'Unknown time'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ').slice(0, 16)
  return date.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })
}

function relativeTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''

  const seconds = Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000))
  if (seconds < 60) return 'just now'
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`
  if (seconds < 604800) return `${Math.floor(seconds / 86400)}d ago`
  return `${Math.floor(seconds / 604800)}w ago`
}

export default function TrackingTimeline({ events = [] }) {
  const newestFirst = [...events].sort(
    (first, second) => new Date(second.recordedAt).getTime() - new Date(first.recordedAt).getTime()
  )

  if (!newestFirst.length) {
    return <p className="text-sm text-slate-500">No tracking updates have been recorded yet.</p>
  }

  return (
    <ol className="ml-2 border-l border-slate-200" aria-label="Tracking timeline">
      {newestFirst.map((event) => (
        <li key={event.id} className="relative pb-6 pl-6 last:pb-0">
          <span
            aria-hidden="true"
            className="absolute -left-1.5 top-1.5 h-3 w-3 rounded-full border-2 border-white bg-brand-600"
          />
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge status={event.status} />
            {event.location && <span className="text-sm font-medium text-slate-900">{event.location}</span>}
          </div>
          {event.notes && <p className="mt-1 text-sm text-slate-600">{event.notes}</p>}
          <p className="mt-1 text-xs text-slate-500">
            {event.recordedByName || 'System'} ·{' '}
            <time dateTime={event.recordedAt} title={formatDateTime(event.recordedAt)}>
              {relativeTime(event.recordedAt)} ({formatDateTime(event.recordedAt)})
            </time>
          </p>
        </li>
      ))}
    </ol>
  )
}
