import { useCallback, useEffect, useState } from 'react'
import { extractErrorMessage } from '../services/api'
import {
  ETA_SOURCE_LABELS,
  etaService,
  formatDelay,
  formatEta,
  riskStyle,
} from '../services/etaService'

/**
 * Predicted arrival, delay risk and the reasoning behind both.
 *
 * The factor list matters as much as the score: an operator will not act on a
 * number they cannot explain to a customer.
 *
 * @param refreshKey change this value to reload, e.g. when a live ping arrives
 */
export default function EtaPanel({ shipmentId, canRecalculate = false, refreshKey = 0 }) {
  const [eta, setEta] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    if (!shipmentId) return
    setLoading(true)
    try {
      setEta(await etaService.getForShipment(shipmentId))
      setError('')
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not load the delivery forecast.'))
    } finally {
      setLoading(false)
    }
  }, [shipmentId])

  useEffect(() => {
    load()
  }, [load, refreshKey])

  async function handleRecalculate() {
    setBusy(true)
    try {
      setEta(await etaService.recalculate(shipmentId))
      setError('')
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not recalculate the forecast.'))
    } finally {
      setBusy(false)
    }
  }

  const risk = riskStyle(eta?.riskLevel)
  const score = eta?.delayRiskScore ?? 0

  return (
    <section className="mt-6 rounded-xl border border-slate-200 bg-white p-6">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
            Delivery forecast
          </h2>
          {eta?.calculatedAt && (
            <p className="mt-1 text-xs text-slate-400">
              Calculated {formatEta(eta.calculatedAt)}
              {eta.source ? ` · ${ETA_SOURCE_LABELS[eta.source] || eta.source}` : ''}
            </p>
          )}
        </div>
        <div className="flex items-center gap-2">
          {eta?.riskLevel && (
            <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${risk.badge}`}>
              {risk.label} · {score}
            </span>
          )}
          {canRecalculate && (
            <button
              type="button"
              onClick={handleRecalculate}
              disabled={busy || loading}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {busy ? 'Recalculating…' : 'Recalculate'}
            </button>
          )}
        </div>
      </div>

      {error && (
        <div role="alert" className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
          {error}
        </div>
      )}

      {loading && !eta && <p className="text-sm text-slate-500">Loading forecast…</p>}

      {eta && (
        <>
          <dl className="grid gap-4 sm:grid-cols-3">
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">Expected arrival</dt>
              <dd className="mt-1 text-sm font-medium text-slate-900">
                {formatEta(eta.predictedDeliveryAt)}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">Promised date</dt>
              <dd className="mt-1 text-sm font-medium text-slate-900">
                {eta.promisedDeliveryDate || '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">Against the promise</dt>
              <dd
                className={`mt-1 text-sm font-medium ${
                  eta.expectedDelayMinutes > 0 ? 'text-red-700' : 'text-emerald-700'
                }`}
              >
                {formatDelay(eta.expectedDelayMinutes)}
              </dd>
            </div>
          </dl>

          <div className="mt-5 grid gap-5 sm:grid-cols-2">
            <div>
              <div className="mb-1.5 flex items-center justify-between text-xs text-slate-500">
                <span className="uppercase tracking-wide">Delay risk</span>
                <span className="font-medium text-slate-700">{score} / 100</span>
              </div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
                <div className={`h-full rounded-full ${risk.bar}`} style={{ width: `${score}%` }} />
              </div>
            </div>
            <div>
              <div className="mb-1.5 flex items-center justify-between text-xs text-slate-500">
                <span className="uppercase tracking-wide">Confidence</span>
                <span className="font-medium text-slate-700">{eta.confidenceScore ?? 0} / 100</span>
              </div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
                <div
                  className="h-full rounded-full bg-brand-500"
                  style={{ width: `${eta.confidenceScore ?? 0}%` }}
                />
              </div>
            </div>
          </div>

          {eta.factors?.length > 0 && (
            <div className="mt-5">
              <h3 className="text-xs uppercase tracking-wide text-slate-500">Why</h3>
              <ul className="mt-2 space-y-1.5">
                {eta.factors.map((factor) => (
                  <li key={factor} className="flex gap-2 text-sm text-slate-600">
                    <span aria-hidden="true" className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-slate-300" />
                    {factor}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
    </section>
  )
}
