import { useCallback, useEffect, useState } from 'react'
import { extractErrorMessage } from '../services/api'
import {
  ETA_SOURCE_LABELS,
  etaService,
  formatEta,
} from '../services/etaService'

function getRisk(score) {
  const value = Number(score ?? 0)

  if (value >= 70) {
    return {
      label: 'High risk',
      badge: 'bg-red-100 text-red-700',
      bar: 'bg-red-500',
    }
  }

  if (value >= 40) {
    return {
      label: 'At risk',
      badge: 'bg-amber-100 text-amber-700',
      bar: 'bg-amber-500',
    }
  }

  return {
    label: 'On track',
    badge: 'bg-emerald-100 text-emerald-700',
    bar: 'bg-emerald-500',
  }
}

function getFactors(factors) {
  if (!factors) return []

  if (Array.isArray(factors)) {
    return factors
  }

  return String(factors)
    .split(';')
    .map((item) => item.trim())
    .filter(Boolean)
}

export default function EtaPanel({
  shipmentId,
  canRecalculate = false,
  refreshKey = 0,
  onEtaChange,
}) {
  const [eta, setEta] = useState(null)
  const [loading, setLoading] = useState(true)
  const [recalculating, setRecalculating] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    if (!shipmentId) return

    setLoading(true)
    setError('')

    try {
      const data = await etaService.getForShipment(
        Number(shipmentId)
      )

      setEta(data)
      onEtaChange?.(data)
    } catch (err) {
      setError(
        extractErrorMessage(
          err,
          'Could not load the delivery forecast.'
        )
      )
    } finally {
      setLoading(false)
    }
  }, [shipmentId, onEtaChange])

  useEffect(() => {
    load()
  }, [load, refreshKey])

  async function handleRecalculate() {
    if (!shipmentId || recalculating) return

    setRecalculating(true)
    setError('')

    try {
      const updatedEta =
        await etaService.recalculate(
          Number(shipmentId)
        )

      console.log(
        'ETA recalculated:',
        updatedEta
      )

      setEta(updatedEta)
      onEtaChange?.(updatedEta)
    } catch (err) {
      console.error(
        'ETA recalculation failed:',
        err
      )

      setError(
        extractErrorMessage(
          err,
          'Could not recalculate delivery forecast.'
        )
      )
    } finally {
      setRecalculating(false)
    }
  }

  const score = Number(
    eta?.delayRiskScore ?? 0
  )

  const confidence = Number(
    eta?.confidenceScore ?? 0
  )

  const risk = getRisk(score)
  const factors = getFactors(eta?.factors)

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
              {eta.source
                ? ` · ${
                    ETA_SOURCE_LABELS[eta.source] ||
                    eta.source
                  }`
                : ''}
            </p>
          )}
        </div>

        <div className="flex items-center gap-2">
          {eta && (
            <span
              className={`rounded-full px-2.5 py-1 text-xs font-medium ${risk.badge}`}
            >
              {risk.label} · {score}
            </span>
          )}

          {canRecalculate && (
            <button
              type="button"
              onClick={handleRecalculate}
              disabled={recalculating}
              className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {recalculating
                ? 'Recalculating…'
                : 'Recalculate'}
            </button>
          )}
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

      {loading && !eta && (
        <p className="text-sm text-slate-500">
          Loading forecast…
        </p>
      )}

      {eta && (
        <>
          <dl className="grid gap-4 sm:grid-cols-3">
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Expected arrival
              </dt>

              <dd className="mt-1 text-sm font-medium text-slate-900">
                {eta.predictedDeliveryTime
                  ? formatEta(
                      eta.predictedDeliveryTime
                    )
                  : '—'}
              </dd>
            </div>

            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Promised date
              </dt>

              <dd className="mt-1 text-sm font-medium text-slate-900">
                {eta.promisedDeliveryDate ||
                  '—'}
              </dd>
            </div>

            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Against the promise
              </dt>

              <dd className="mt-1 text-sm font-medium text-emerald-700">
                {eta.expectedDelayMinutes != null
                  ? `${eta.expectedDelayMinutes} minutes`
                  : 'Forecast available'}
              </dd>
            </div>
          </dl>

          <div className="mt-5 grid gap-5 sm:grid-cols-2">
            <div>
              <div className="mb-1.5 flex items-center justify-between text-xs text-slate-500">
                <span className="uppercase tracking-wide">
                  Delay risk
                </span>

                <span className="font-medium text-slate-700">
                  {score} / 100
                </span>
              </div>

              <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
                <div
                  className={`h-full rounded-full ${risk.bar}`}
                  style={{
                    width: `${Math.min(
                      score,
                      100
                    )}%`,
                  }}
                />
              </div>
            </div>

            <div>
              <div className="mb-1.5 flex items-center justify-between text-xs text-slate-500">
                <span className="uppercase tracking-wide">
                  Confidence
                </span>

                <span className="font-medium text-slate-700">
                  {confidence} / 100
                </span>
              </div>

              <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
                <div
                  className="h-full rounded-full bg-brand-500"
                  style={{
                    width: `${Math.min(
                      confidence,
                      100
                    )}%`,
                  }}
                />
              </div>
            </div>
          </div>

          {factors.length > 0 && (
            <div className="mt-5">
              <h3 className="text-xs uppercase tracking-wide text-slate-500">
                Why
              </h3>

              <ul className="mt-2 space-y-1.5">
                {factors.map(
                  (factor, index) => (
                    <li
                      key={`${factor}-${index}`}
                      className="flex gap-2 text-sm text-slate-600"
                    >
                      <span
                        aria-hidden="true"
                        className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-slate-300"
                      />

                      {factor}
                    </li>
                  )
                )}
              </ul>
            </div>
          )}
        </>
      )}
    </section>
  )
}