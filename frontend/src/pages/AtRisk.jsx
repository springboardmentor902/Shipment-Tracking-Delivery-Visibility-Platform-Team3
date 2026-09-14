import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import AppLayout from '../components/AppLayout'
import StatusBadge from '../components/StatusBadge'
import { extractErrorMessage } from '../services/api'
import { etaService, formatDelay, formatEta, riskStyle } from '../services/etaService'

const THRESHOLDS = [
  { value: 1, label: 'Watch and above' },
  { value: 4, label: 'At risk and above' },
  { value: 7, label: 'High risk and above' },
  { value: 9, label: 'Critical only' },
]

function getRiskLevel(score) {
  if (score >= 9) return 'CRITICAL'
  if (score >= 7) return 'HIGH'
  if (score >= 4) return 'MEDIUM'
  return 'LOW'
}

/**
 * Deliveries likely to miss their promised date, worst first.
 */
export default function AtRisk() {
  const [rows, setRows] = useState([])
  const [minScore, setMinScore] = useState(7)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)

    try {
      const result = await etaService.listAtRisk(minScore)

      console.log('Delay watch response:', result)

      setRows(Array.isArray(result) ? result : [])
      setError('')
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not load the delay watch list.'))
    } finally {
      setLoading(false)
    }
  }, [minScore])

  useEffect(() => {
    load()
  }, [load])

  return (
    <AppLayout>
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">
            Delay watch
          </h1>

          <p className="mt-1 text-sm text-slate-500">
            Shipments whose forecast puts them behind the promised date,
            highest risk first.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <label
            htmlFor="minScore"
            className="text-sm text-slate-500"
          >
            Threshold
          </label>

          <select
            id="minScore"
            value={minScore}
            onChange={(event) => setMinScore(Number(event.target.value))}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
          >
            {THRESHOLDS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <button
            type="button"
            onClick={load}
            disabled={loading}
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-white disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? 'Refreshing…' : 'Refresh'}
          </button>
        </div>
      </div>

      {error && (
        <div
          role="alert"
          className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700"
        >
          {error}
        </div>
      )}

      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th className="px-4 py-3 font-medium">Tracking</th>
              <th className="px-4 py-3 font-medium">Risk</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Receiver</th>
              <th className="px-4 py-3 font-medium">Expected arrival</th>
              <th className="px-4 py-3 font-medium">Against promise</th>
              <th className="px-4 py-3 font-medium">Confidence</th>
            </tr>
          </thead>

          <tbody className="divide-y divide-slate-100">
            {loading && rows.length === 0 && (
              <tr>
                <td
                  colSpan={7}
                  className="px-4 py-10 text-center text-slate-500"
                >
                  Loading…
                </td>
              </tr>
            )}

            {!loading && rows.length === 0 && (
              <tr>
                <td
                  colSpan={7}
                  className="px-4 py-10 text-center text-slate-500"
                >
                  Nothing above this threshold. Every tracked delivery is on course.
                </td>
              </tr>
            )}

           {rows.map((row) => {
  const score = Number(row.delayRiskScore ?? 0)

  const riskLevel =
    score >= 9
      ? 'CRITICAL'
      : score >= 7
        ? 'HIGH'
        : score >= 4
          ? 'MEDIUM'
          : 'LOW'

  const risk = riskStyle(riskLevel)

  return (
    <tr
      key={row.id}
      className="hover:bg-slate-50"
    >
      <td className="px-4 py-3 font-mono text-xs">
        <Link
          to={`/shipments/${row.shipmentId}`}
          className="text-brand-600 hover:text-brand-700"
        >
          Shipment #{row.shipmentId}
        </Link>
      </td>

      <td className="px-4 py-3">
        <span
          className={`rounded-full px-2.5 py-1 text-xs font-medium ${risk.badge}`}
        >
          {risk.label} · {score}
        </span>
      </td>

      <td className="px-4 py-3">
        <StatusBadge status={row.status || 'UNKNOWN'} />
      </td>

      <td className="px-4 py-3 text-slate-700">
        {row.receiverName || '—'}
      </td>

      <td className="px-4 py-3 text-slate-700">
        {formatEta(row.predictedDeliveryTime)}
      </td>

      <td className="px-4 py-3 text-red-700">
        {row.expectedDelayMinutes == null
          ? '—'
          : formatDelay(row.expectedDelayMinutes)}
      </td>

      <td className="px-4 py-3 text-slate-500">
        {row.confidenceScore ?? 0}%
      </td>
    </tr>
  )
})}
          </tbody>
        </table>
      </div>
    </AppLayout>
  )
}