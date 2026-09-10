import { enableNotifications } from "../services/fcmService";

function EnableAlertsButton() {
  const handleEnableAlerts = async () => {
    try {
      const token = await enableNotifications();

      console.log("FCM token:", token);

      // Next: POST this token to your Spring Boot backend.
      // await api.post("/notifications/fcm-token", { token });
    } catch (error) {
      console.error(error.message);
    }
  };

  return <button onClick={handleEnableAlerts}>Enable shipment alerts</button>;
}

export default EnableAlertsButton;