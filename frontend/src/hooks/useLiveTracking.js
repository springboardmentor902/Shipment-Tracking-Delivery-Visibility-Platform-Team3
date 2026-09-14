import { Client } from "@stomp/stompjs";
import { useEffect, useRef, useState } from "react";

const TOKEN_KEY = "shiptrack_token";

function resolveTrackingSocketUrl() {
  const configuredUrl = import.meta.env.VITE_WS_URL;

  if (configuredUrl) {
    return configuredUrl;
  }

  const apiBaseUrl =
    import.meta.env.VITE_API_BASE_URL ||
    "http://localhost:8080/api";

  const url = new URL(
    apiBaseUrl,
    window.location.origin
  );

  url.protocol =
    url.protocol === "https:" ? "wss:" : "ws:";

  url.pathname =
    `${url.pathname.replace(/\/+$/, "")}/ws/tracking`;

  url.search = "";

  return url.toString();
}

export default function useLiveTracking({
  destination,
  onUpdate,
  enabled = true,
}) {
  const [status, setStatus] = useState("idle");
  const [error, setError] = useState("");
  const [lastUpdate, setLastUpdate] = useState(null);

  const handlerRef = useRef(onUpdate);

  useEffect(() => {
    handlerRef.current = onUpdate;
  }, [onUpdate]);

  useEffect(() => {
    if (!enabled || !destination) {
      setStatus("idle");
      return undefined;
    }

    const token =
      localStorage.getItem(TOKEN_KEY);

    if (!token) {
      setStatus("error");
      setError(
        "Sign in again to see live updates."
      );

      return undefined;
    }

    let subscription = null;

    setStatus("connecting");
    setError("");

    const client = new Client({
      brokerURL: resolveTrackingSocketUrl(),

      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },

      reconnectDelay: 5000,

      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      debug: (message) => {
        console.log("[STOMP]", message);
      },

      onConnect: () => {
        console.log(
          "Tracking WebSocket connected"
        );

        setStatus("live");
        setError("");

        subscription = client.subscribe(
          destination,
          (message) => {
            try {
              const update = JSON.parse(
                message.body
              );

              setLastUpdate(update);

              if (handlerRef.current) {
                handlerRef.current(update);
              }
            } catch (parseError) {
              console.error(
                "Invalid live tracking message:",
                parseError
              );
            }
          }
        );
      },

      onStompError: (frame) => {
        console.error(
          "STOMP error:",
          frame.headers?.message,
          frame.body
        );

        setStatus("error");

        setError(
          frame.headers?.message ||
            "The live tracking feed refused the connection."
        );
      },

      onWebSocketClose: () => {
        setStatus((previousStatus) => {
          if (previousStatus === "error") {
            return previousStatus;
          }

          return "reconnecting";
        });
      },

      onWebSocketError: (socketError) => {
        console.error(
          "WebSocket error:",
          socketError
        );
      },
    });

    client.activate();

    return () => {
      try {
        if (subscription) {
          subscription.unsubscribe();
        }
      } catch (unsubscribeError) {
        console.error(
          "Subscription cleanup error:",
          unsubscribeError
        );
      }

      client.deactivate();
      setStatus("idle");
    };
  }, [destination, enabled]);

  return {
    status,
    error,
    lastUpdate,
  };
}