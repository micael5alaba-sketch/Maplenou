/*
 * Maplenou — service worker for browser push notifications (Firebase Cloud Messaging, docs/guide/firebase.md).
 * It only displays pushes and opens the app on click: no caching, no Firebase SDK, no network access.
 * The backend sends title/body both in the FCM "notification" block and in "data".
 */
self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (event) => event.waitUntil(self.clients.claim()));

function readPayload(event) {
  if (!event.data) return {};
  try {
    return event.data.json();
  } catch {
    return { notification: { body: event.data.text() } };
  }
}

self.addEventListener("push", (event) => {
  const payload = readPayload(event);
  const data = payload.data || {};
  const notification = payload.notification || {};
  const title = notification.title || data.title || "Maplenou";
  const options = {
    body: notification.body || data.body || "",
    icon: "/icon.svg",
    badge: "/icon.svg",
    tag: data.ticketId ? `ticket-${data.ticketId}` : undefined,
    data: { ticketId: data.ticketId || null, type: data.type || null },
  };
  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const ticketId = event.notification.data && event.notification.data.ticketId;
  // "/" redirects to the right screen for the signed-in role (requester or support)
  const target = new URL(ticketId ? `/?ticket=${encodeURIComponent(ticketId)}` : "/", self.location.origin).href;
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((windows) => {
      for (const w of windows) {
        if (new URL(w.url).origin === self.location.origin && "focus" in w) {
          return w.navigate(target).then((c) => (c || w).focus());
        }
      }
      return self.clients.openWindow(target);
    }),
  );
});
