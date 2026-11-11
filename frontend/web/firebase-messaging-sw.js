/*
 * Service worker des notifications push du navigateur (Firebase Cloud Messaging).
 *
 * Il reçoit les notifications quand l'onglet Maplenou est en arrière-plan ou fermé et affiche le
 * pop-up du système (comme une application normale). Au clic sur le pop-up, il ouvre l'application
 * en lui passant la charge utile dans le paramètre d'URL "notif" (JSON en base64 URL-safe) :
 * l'app ouvre alors directement l'écran concerné (discussion, commande, livraison...).
 *
 * La configuration ci-dessous est la configuration PUBLIQUE du projet Firebase web (identique à
 * celle déjà embarquée dans l'application), pas un secret.
 */
importScripts('https://www.gstatic.com/firebasejs/10.14.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.14.1/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: 'AIzaSyAfY8hXrj_7V7v926bpWuJY6_ycXtr3II4',
  appId: '1:833530565751:web:4bb53f36ab5571a2ac2272',
  messagingSenderId: '833530565751',
  projectId: 'maplenou-c21c2',
  authDomain: 'maplenou-c21c2.firebaseapp.com',
  storageBucket: 'maplenou-c21c2.firebasestorage.app',
});

const messaging = firebase.messaging();

// Quand la charge utile contient déjà "notification" (c'est le cas : titre + texte), Firebase
// affiche lui-même le pop-up. On garde ce handler pour les messages "data" seuls.
messaging.onBackgroundMessage((payload) => {
  if (payload.notification) return;
  const data = payload.data || {};
  self.registration.showNotification(data.title || 'Maplenou', {
    body: data.body || '',
    icon: '/icons/Icon-192.png',
    data,
  });
});

function encodePayload(data) {
  const json = JSON.stringify(data || {});
  const bytes = new TextEncoder().encode(json);
  let binary = '';
  bytes.forEach((b) => (binary += String.fromCharCode(b)));
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

// Clic sur le pop-up : ouvre (ou ramène au premier plan) l'application sur l'écran concerné.
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const data = (event.notification.data && (event.notification.data.FCM_MSG
    ? event.notification.data.FCM_MSG.data
    : event.notification.data)) || {};
  const target = self.location.origin + '/?notif=' + encodePayload(data);

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
      for (const client of windowClients) {
        if (client.url.startsWith(self.location.origin) && 'navigate' in client) {
          return client.navigate(target).then((c) => c && c.focus());
        }
      }
      return clients.openWindow(target);
    }),
  );
});
