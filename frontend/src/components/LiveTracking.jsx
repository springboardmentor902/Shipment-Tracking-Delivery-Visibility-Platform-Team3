import { useEffect, useState } from "react";
import { connectTrackingWebSocket } from "../services/websocket";

export default function LiveTracking({ vehicleId }) {
  const [location, setLocation] = useState(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (!vehicleId) {
      setConnected(false);
      return undefined;
    }

    const disconnect = connectTrackingWebSocket(
      vehicleId,
      (newLocation) => {
        setLocation(newLocation);
      },
      () => {
        setConnected(true);
      },
      () => {
        setConnected(false);
      }
    );

    return () => {
      disconnect();
      setConnected(false);
    };
  }, [vehicleId]);

  return (
    <div>
      <p>
        Status:{" "}
        {connected ? "Connected" : "Disconnected"}
      </p>

      {location && (
        <div>
          <p>Latitude: {location.latitude}</p>
          <p>Longitude: {location.longitude}</p>
        </div>
      )}
    </div>
  );
}