importScripts("https://www.gstatic.com/firebasejs/10.14.1/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.14.1/firebase-messaging-compat.js");

firebase.initializeApp({
  apiKey:"AIzaSyBKGLrD_e2AH_uY-WnudmKn0exaKocrMe4",
  authDomain: "shiptrack-pro-firebase-s-eee77.firebaseapp.com",
  projectId: "shiptrack-pro-firebase-s-eee77",
  storageBucket: "shiptrack-pro-firebase-s-eee77.firebasestorage.app",
  messagingSenderId: "729611964632",
  appId: "1:729611964632:web:0bcc9fda20354f535307ad",
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const title = payload.notification?.title || "ShipTrack";
  const options = {
    body: payload.notification?.body || "You have a new shipment update.",
    icon: "/favicon.ico",
  };

  self.registration.showNotification(title, options);
});