importScripts("https://www.gstatic.com/firebasejs/10.14.1/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.14.1/firebase-messaging-compat.js");

firebase.initializeApp({
  apiKey: "AIzaSyCzsfd0PJjnY-W09dtAveRHUZ19OJBuB0M",
  authDomain: "shiptrack-pro-firebase-setup.firebaseapp.com",
  projectId: "shiptrack-pro-firebase-setup",
  storageBucket: "shiptrack-pro-firebase-setup.firebasestorage.app",
  messagingSenderId: "1071054434317",
  appId: "1:1071054434317:web:585f94573e2f6a71c56544",
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