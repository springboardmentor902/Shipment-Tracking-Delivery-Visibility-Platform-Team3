const STATES = {
  idle: { label: 'Live off', dot: 'bg-slate-400', text: 'text-slate-600 bg-slate-100' },
  connecting: { label: 'Connecting', dot: 'bg-amber-500', text: 'text-amber-700 bg-amber-50' },
  live: { label: 'Live', dot: 'bg-emerald-500', text: 'text-emerald-700 bg-emerald-50' },
  reconnecting: { label: 'Reconnecting', dot: 'bg-amber-500', text: 'text-amber-700 bg-amber-50' },
  error: { label: 'Live unavailable', dot: 'bg-red-500', text: 'text-red-700 bg-red-50' },
}

/** Small indicator for the state of the live tracking socket. */
export default function LiveStatusPill({ status = 'idle', error = '', updatedAt = null }) {
  const state = STATES[status] || STATES.idle
  const stamp = updatedAt
    ? new Date(updatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    : null

  return (
    <span
      title={error || (stamp ? `Last update ${stamp}` : state.label)}
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${state.text}`}
    >
      <span
        className={`h-2 w-2 rounded-full ${state.dot} ${status === 'live' ? 'animate-pulse' : ''}`}
        aria-hidden="true"
      />
      {state.label}
      {status === 'live' && stamp && <span className="font-normal text-emerald-600">· {stamp}</span>}
    </span>
  )
}
