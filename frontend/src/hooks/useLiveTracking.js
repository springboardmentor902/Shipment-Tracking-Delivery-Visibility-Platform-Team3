import { Client } from '@stomp/stompjs'
import { useEffect, useRef, useState } from 'react'
import { TOKEN_KEY } from '../services/api'

/**
 * Turns the REST base URL into the WebSocket URL of the tracking endpoint,
 * e.g. http://localhost:8081/api -> ws://localhost:8081/api/ws/tracking.
 */
export function resolveTrackingSocketUrl() {
  const configured = import.meta.env.VITE_WS_URL
  if (configured) return configured

  const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api'
  const url = new URL(apiBase, window.location.origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = `${url.pathname.replace(/\/+$/, '')}/ws/tracking`
  url.search = ''
  return url.toString()
}

/**
 * Subscribes to a live tracking topic over STOMP.
 *
 * The JWT travels on the CONNECT frame because browsers cannot set headers on a
 * WebSocket handshake. The client reconnects on its own, and the subscription is
 * always torn down when the component unmounts or the destination changes, so
 * leaving the page stops the traffic.
 *
 * @returns {{status: string, error: string, lastUpdate: object|null}}
 *   status is one of idle, connecting, live, reconnecting, error.
 */
export default function useLiveTracking({ destination, onUpdate, enabled = true }) {
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')
  const [lastUpdate, setLastUpdate] = useState(null)

  // Keeping the callback in a ref means a new inline function on every render
  // does not tear the socket down and rebuild it.
  const handlerRef = useRef(onUpdate)
  useEffect(() => {
    handlerRef.current = onUpdate
  }, [onUpdate])

  useEffect(() => {
    if (!enabled || !destination) {
      setStatus('idle')
      return undefined
    }

    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      setStatus('error')
      setError('Sign in again to see live updates.')
      return undefined
    }

    let subscription = null
    setStatus('connecting')
    setError('')

    const client = new Client({
      brokerURL: resolveTrackingSocketUrl(),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setStatus('live')
        setError('')
        subscription = client.subscribe(destination, (message) => {
          try {
            const update = JSON.parse(message.body)
            setLastUpdate(update)
            handlerRef.current?.(update)
          } catch {
            // a malformed frame should not kill the socket
          }
        })
      },
      onStompError: (frame) => {
        // the server refuses the connection or the subscription
        setStatus('error')
        setError(frame.headers?.message || 'The live tracking feed refused the connection.')
      },
      onWebSocketClose: () => {
        setStatus((previous) => (previous === 'error' ? previous : 'reconnecting'))
      },
    })

    client.activate()

    return () => {
      try {
        subscription?.unsubscribe()
      } catch {
        // the socket may already be gone
      }
      client.deactivate()
      setStatus('idle')
    }
  }, [destination, enabled])

  return { status, error, lastUpdate }
}
