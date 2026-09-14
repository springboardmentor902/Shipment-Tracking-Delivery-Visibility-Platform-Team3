import { getToken, onMessage } from "firebase/messaging";
import { messaging } from "../firebase";

export async function enableNotifications() {
  const permission = await Notification.requestPermission();

  if (permission !== "granted") {
    throw new Error("Notification permission was not granted.");
  }

  const registration = await navigator.serviceWorker.register(
    "/firebase-messaging-sw.js"
  );

  const token = await getToken(messaging, {
    vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY,
    serviceWorkerRegistration: registration,
  });

  if (!token) {
    throw new Error("FCM token could not be created.");
  }

  return token;
}

export function listenForForegroundMessages(callback) {
  return onMessage(messaging, callback);
}