import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const API_URL = "http://localhost:8080";

export const connectTrackingWebSocket = (
  vehicleId,
  onLocationUpdate,
  onConnected,
  onError
) => {
  const stompClient = new Client({
    webSocketFactory: () =>
      new SockJS(`${API_URL}/api/ws/tracking`),

    reconnectDelay: 5000,

    onConnect: () => {
      console.log("Tracking WebSocket connected");

      onConnected?.();

      stompClient.subscribe(
        `/topic/tracking/${vehicleId}`,
        (message) => {
          try {
            const location = JSON.parse(message.body);

            console.log("Live location:", location);

            onLocationUpdate?.(location);
          } catch (error) {
            console.error(
              "Invalid WebSocket message:",
              error
            );
          }
        }
      );
    },

    onStompError: (frame) => {
      console.error("STOMP error:", frame);

      onError?.(frame);
    },

    onWebSocketError: (error) => {
      console.error("WebSocket error:", error);

      onError?.(error);
    },

    onWebSocketClose: () => {
      console.log("Tracking WebSocket disconnected");
    },
  });

  stompClient.activate();

  return () => {
    stompClient.deactivate();
  };
};