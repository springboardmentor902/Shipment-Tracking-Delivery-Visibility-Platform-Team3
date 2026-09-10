const STYLES = {
  CREATED: 'bg-slate-100 text-slate-700 ring-slate-200',
  PICKED_UP: 'bg-blue-50 text-blue-700 ring-blue-200',
  IN_TRANSIT: 'bg-indigo-50 text-indigo-700 ring-indigo-200',
  OUT_FOR_DELIVERY: 'bg-amber-50 text-amber-700 ring-amber-200',
  DELIVERED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  FAILED_DELIVERY: 'bg-red-50 text-red-700 ring-red-200',
  CANCELLED: 'bg-slate-200 text-slate-600 ring-slate-300',
}

export default function StatusBadge({ status }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium
                  ring-1 ring-inset ${STYLES[status] || STYLES.CREATED}`}
    >
      {status?.replaceAll('_', ' ')}
    </span>
  )
}
