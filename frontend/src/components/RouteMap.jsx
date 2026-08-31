import { useEffect, useMemo, useRef, useState } from 'react'

const MAPS_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || ''

let loaderPromise = null

/**
 * Loads the Google Maps JS API once per page. Rejects when no key is set or the
 * script cannot be fetched, so callers can render a fallback instead.
 */
function loadGoogleMaps() {
  if (window.google?.maps) return Promise.resolve(window.google.maps)
  if (!MAPS_KEY) return Promise.reject(new Error('missing-key'))
  if (loaderPromise) return loaderPromise

  loaderPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(MAPS_KEY)}`
    script.async = true
    script.defer = true
    script.onload = () =>
      window.google?.maps ? resolve(window.google.maps) : reject(new Error('load-failed'))
    script.onerror = () => {
      loaderPromise = null
      reject(new Error('load-failed'))
    }
    document.head.appendChild(script)
  })
  return loaderPromise
}

function toNumber(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

/** Pulls every drawable point out of the legs the backend returned. */
function collectPoints(legs) {
  return legs.map((leg) => {
    const origin =
      toNumber(leg.originLatitude) !== null && toNumber(leg.originLongitude) !== null
        ? { lat: toNumber(leg.originLatitude), lng: toNumber(leg.originLongitude) }
        : null
    const destination =
      toNumber(leg.destinationLatitude) !== null && toNumber(leg.destinationLongitude) !== null
        ? { lat: toNumber(leg.destinationLatitude), lng: toNumber(leg.destinationLongitude) }
        : null
    const driver =
      toNumber(leg.lastKnownLatitude) !== null && toNumber(leg.lastKnownLongitude) !== null
        ? { lat: toNumber(leg.lastKnownLatitude), lng: toNumber(leg.lastKnownLongitude) }
        : null
    return { leg, origin, destination, driver }
  })
}

/**
 * Route map for a shipment's legs.
 *
 * Falls back to a readable coordinate summary whenever the browser Maps key is
 * absent, the script is blocked, or the legs have not been geocoded yet — the
 * page must never look broken just because Maps is unavailable.
 */
export default function RouteMap({ legs = [], livePosition = null, height = 320 }) {
  const containerRef = useRef(null)
  const [failure, setFailure] = useState(MAPS_KEY ? '' : 'missing-key')

  const points = useMemo(() => collectPoints(legs), [legs])
  // a ping that just arrived over the socket, not yet reflected in the legs
  const live = useMemo(() => {
    if (!livePosition) return null
    const lat = toNumber(livePosition.latitude)
    const lng = toNumber(livePosition.longitude)
    return lat === null || lng === null ? null : { lat, lng, label: livePosition.location }
  }, [livePosition])

  const hasCoordinates =
    Boolean(live) || points.some((item) => item.origin || item.destination || item.driver)

  useEffect(() => {
    if (!hasCoordinates || !MAPS_KEY) return
    let cancelled = false

    loadGoogleMaps()
      .then((maps) => {
        if (cancelled || !containerRef.current) return

        const map = new maps.Map(containerRef.current, {
          zoom: 6,
          center:
            live || points[0]?.origin || points[0]?.destination || points[0]?.driver || { lat: 20.6, lng: 78.9 },
          mapTypeControl: false,
          streetViewControl: false,
        })
        const bounds = new maps.LatLngBounds()

        points.forEach(({ leg, origin, destination, driver }) => {
          if (origin) {
            new maps.Marker({
              map,
              position: origin,
              label: String(leg.legNumber ?? ''),
              title: `Leg ${leg.legNumber} start · ${leg.originAddress || ''}`,
            })
            bounds.extend(origin)
          }
          if (destination) {
            new maps.Marker({
              map,
              position: destination,
              title: `Leg ${leg.legNumber} end · ${leg.destinationAddress || ''}`,
            })
            bounds.extend(destination)
          }
          if (origin && destination) {
            new maps.Polyline({
              map,
              path: [origin, destination],
              strokeColor: leg.status === 'COMPLETED' ? '#94a3b8' : '#2563eb',
              strokeOpacity: 0.9,
              strokeWeight: 4,
            })
          }
          if (driver) {
            new maps.Marker({
              map,
              position: driver,
              title: `Last known position · ${leg.driverName || 'driver'}`,
              icon: {
                path: maps.SymbolPath.CIRCLE,
                scale: 7,
                fillColor: '#16a34a',
                fillOpacity: 1,
                strokeColor: '#ffffff',
                strokeWeight: 2,
              },
            })
            bounds.extend(driver)
          }
        })

        if (live) {
          new maps.Marker({
            map,
            position: live,
            zIndex: 999,
            title: `Live position${live.label ? ` · ${live.label}` : ''}`,
            icon: {
              path: maps.SymbolPath.CIRCLE,
              scale: 9,
              fillColor: '#dc2626',
              fillOpacity: 1,
              strokeColor: '#ffffff',
              strokeWeight: 3,
            },
          })
          bounds.extend(live)
        }

        if (!bounds.isEmpty()) {
          map.fitBounds(bounds, 48)
        }
      })
      .catch((error) => {
        if (!cancelled) setFailure(error.message || 'load-failed')
      })

    return () => {
      cancelled = true
    }
  }, [hasCoordinates, points, live])

  if (!hasCoordinates) {
    return (
      <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600">
        No coordinates yet. Add a route leg with an origin and destination address — the server
        geocodes them, or you can type the latitude and longitude yourself.
      </div>
    )
  }

  if (failure) {
    return (
      <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600">
        <p className="font-medium text-slate-800">
          {failure === 'missing-key'
            ? 'Map preview is off because VITE_GOOGLE_MAPS_API_KEY is not set.'
            : 'Google Maps could not be loaded, showing coordinates instead.'}
        </p>
        {live && (
          <p className="mt-2 font-mono text-xs text-red-700">
            live position {live.lat}, {live.lng}
            {live.label ? ` · ${live.label}` : ''}
          </p>
        )}
        <ul className="mt-3 space-y-2">
          {points.map(({ leg, origin, destination, driver }) => (
            <li key={leg.id} className="rounded border border-slate-200 bg-white p-2.5">
              <p className="font-medium text-slate-900">
                Leg {leg.legNumber}: {leg.originAddress} → {leg.destinationAddress}
              </p>
              <p className="mt-1 font-mono text-xs text-slate-500">
                {origin ? `${origin.lat}, ${origin.lng}` : 'origin not geocoded'} →{' '}
                {destination ? `${destination.lat}, ${destination.lng}` : 'destination not geocoded'}
              </p>
              {driver && (
                <p className="mt-1 font-mono text-xs text-emerald-700">
                  driver at {driver.lat}, {driver.lng}
                </p>
              )}
            </li>
          ))}
        </ul>
      </div>
    )
  }

  return (
    <div
      ref={containerRef}
      style={{ height }}
      className="w-full overflow-hidden rounded-lg border border-slate-200 bg-slate-100"
      aria-label="Route map"
    />
  )
}
