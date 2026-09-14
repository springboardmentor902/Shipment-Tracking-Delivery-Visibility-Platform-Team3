import { useEffect, useRef, useState } from 'react'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import VectorLayer from 'ol/layer/Vector'
import OSM from 'ol/source/OSM'
import VectorSource from 'ol/source/Vector'
import Feature from 'ol/Feature'
import Point from 'ol/geom/Point'
import LineString from 'ol/geom/LineString'
import { fromLonLat } from 'ol/proj'
import { Style, Circle, Fill, Stroke } from 'ol/style'
import 'ol/ol.css'

function toNumber(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

async function geocode(address) {
  if (!address) return null

  const response = await fetch(
    `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(address)}`
  )

  if (!response.ok) {
    throw new Error('Could not geocode address.')
  }

  const data = await response.json()

  if (!data.length) {
    return null
  }

  return {
    latitude: Number(data[0].lat),
    longitude: Number(data[0].lon),
  }
}

async function getRoute(origin, destination) {
  const url =
    `https://router.project-osrm.org/route/v1/driving/` +
    `${origin.longitude},${origin.latitude};` +
    `${destination.longitude},${destination.latitude}` +
    `?overview=full&geometries=geojson`

  const response = await fetch(url)

  if (!response.ok) {
    throw new Error('Could not calculate route.')
  }

  const data = await response.json()

  if (data.code !== 'Ok' || !data.routes?.length) {
    throw new Error('No driving route found.')
  }

  return data.routes[0]
}

function createMarkerStyle(type) {
  let fillColor = '#2563eb'

  if (type === 'origin') {
    fillColor = '#16a34a'
  }

  if (type === 'destination') {
    fillColor = '#2563eb'
  }

  if (type === 'driver') {
    fillColor = '#dc2626'
  }

  return new Style({
    image: new Circle({
      radius: 8,
      fill: new Fill({
        color: fillColor,
      }),
      stroke: new Stroke({
        color: '#ffffff',
        width: 3,
      }),
    }),
  })
}

export default function RouteMap({
  legs = [],
  livePosition = null,
  height = 320,
}) {
  const mapElementRef = useRef(null)
  const mapRef = useRef(null)
  const vectorSourceRef = useRef(null)

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!mapElementRef.current || mapRef.current) {
      return
    }

    const vectorSource = new VectorSource()

    vectorSourceRef.current = vectorSource

    const vectorLayer = new VectorLayer({
      source: vectorSource,
    })

    const map = new Map({
      target: mapElementRef.current,

      layers: [
        new TileLayer({
          source: new OSM(),
        }),
        vectorLayer,
      ],

      view: new View({
        center: fromLonLat([78.9629, 20.5937]),
        zoom: 5,
      }),
    })

    mapRef.current = map

    return () => {
      map.setTarget(undefined)
      mapRef.current = null
    }
  }, [])

  useEffect(() => {
    async function buildMap() {
      if (!mapRef.current || !vectorSourceRef.current) {
        return
      }

      vectorSourceRef.current.clear()

      setError('')

      if (!legs.length && !livePosition) {
        return
      }

      setLoading(true)

      try {
        const allCoordinates = []

        for (const leg of legs) {
          let origin = null
          let destination = null

          // Use coordinates if backend already provides them
          const originLat = toNumber(leg.originLatitude)
          const originLng = toNumber(leg.originLongitude)

          const destinationLat = toNumber(
            leg.destinationLatitude
          )
          const destinationLng = toNumber(
            leg.destinationLongitude
          )

          if (
            originLat !== null &&
            originLng !== null
          ) {
            origin = {
              latitude: originLat,
              longitude: originLng,
            }
          } else if (leg.origin) {
            origin = await geocode(leg.origin)
          } else if (leg.originAddress) {
            origin = await geocode(leg.originAddress)
          }

          if (
            destinationLat !== null &&
            destinationLng !== null
          ) {
            destination = {
              latitude: destinationLat,
              longitude: destinationLng,
            }
          } else if (leg.destination) {
            destination = await geocode(
              leg.destination
            )
          } else if (leg.destinationAddress) {
            destination = await geocode(
              leg.destinationAddress
            )
          }

          if (!origin || !destination) {
            continue
          }

          const originPoint = fromLonLat([
            origin.longitude,
            origin.latitude,
          ])

          const destinationPoint = fromLonLat([
            destination.longitude,
            destination.latitude,
          ])

          const originFeature = new Feature({
            geometry: new Point(originPoint),
          })

          originFeature.setStyle(
            createMarkerStyle('origin')
          )

          const destinationFeature = new Feature({
            geometry: new Point(destinationPoint),
          })

          destinationFeature.setStyle(
            createMarkerStyle('destination')
          )

          vectorSourceRef.current.addFeature(
            originFeature
          )

          vectorSourceRef.current.addFeature(
            destinationFeature
          )

          allCoordinates.push(originPoint)
          allCoordinates.push(destinationPoint)

          // Get actual road route from OSRM
          try {
            const route = await getRoute(
              origin,
              destination
            )

            const routeCoordinates =
              route.geometry.coordinates.map(
                ([longitude, latitude]) =>
                  fromLonLat([
                    longitude,
                    latitude,
                  ])
              )

            const routeFeature = new Feature({
              geometry: new LineString(
                routeCoordinates
              ),
            })

            routeFeature.setStyle(
              new Style({
                stroke: new Stroke({
                  color:
                    leg.status === 'COMPLETED'
                      ? '#94a3b8'
                      : '#2563eb',
                  width: 5,
                }),
              })
            )

            vectorSourceRef.current.addFeature(
              routeFeature
            )

            allCoordinates.push(
              ...routeCoordinates
            )
          } catch (routeError) {
            console.warn(
              'OSRM route calculation failed:',
              routeError
            )

            // Draw straight line as fallback
            const fallbackLine = new Feature({
              geometry: new LineString([
                originPoint,
                destinationPoint,
              ]),
            })

            fallbackLine.setStyle(
              new Style({
                stroke: new Stroke({
                  color: '#2563eb',
                  width: 4,
                  lineDash: [8, 8],
                }),
              })
            )

            vectorSourceRef.current.addFeature(
              fallbackLine
            )
          }
        }

        // Live driver position
        if (livePosition) {
          const lat = toNumber(
            livePosition.latitude
          )

          const lng = toNumber(
            livePosition.longitude
          )

          if (lat !== null && lng !== null) {
            const driverPoint = fromLonLat([
              lng,
              lat,
            ])

            const driverFeature = new Feature({
              geometry: new Point(driverPoint),
            })

            driverFeature.setStyle(
              createMarkerStyle('driver')
            )

            vectorSourceRef.current.addFeature(
              driverFeature
            )

            allCoordinates.push(driverPoint)
          }
        }

        // Fit map to route
        if (allCoordinates.length) {
          const extent =
            vectorSourceRef.current.getExtent()

          mapRef.current.getView().fit(extent, {
            padding: [40, 40, 40, 40],
            maxZoom: 12,
            duration: 500,
          })
        }
      } catch (err) {
        console.error(err)

        setError(
          err.message ||
            'Could not load the OpenStreetMap route.'
        )
      } finally {
        setLoading(false)
      }
    }

    buildMap()
  }, [legs, livePosition])

  return (
    <div
      style={{ height }}
      className="relative w-full overflow-hidden rounded-lg border border-slate-200"
    >
      <div
        ref={mapElementRef}
        style={{
          width: '100%',
          height: '100%',
        }}
      />

      {loading && (
        <div className="absolute left-3 top-3 rounded-md bg-white px-3 py-2 text-sm shadow">
          Loading route…
        </div>
      )}

      {error && (
        <div className="absolute left-3 right-3 top-3 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700 shadow">
          {error}
        </div>
      )}

      <div className="absolute bottom-0 left-0 right-0 bg-white px-3 py-2 text-xs text-slate-500">
        © OpenStreetMap contributors
      </div>
    </div>
  )
}