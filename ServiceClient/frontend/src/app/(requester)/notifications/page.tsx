"use client";

import { NotificationList } from "@/components/NotificationList";
import { PushNotifications } from "@/components/PushNotifications";
import { useMessages } from "@/lib/i18n/client";
import { requesterNotifications } from "@/lib/i18n/messages/requester";

export default function NotificationsPage() {
  const t = useMessages(requesterNotifications);
  return (
    <div className="stack">
      <h1>{t.title}</h1>
      <PushNotifications />
      <NotificationList ticketHref={(id) => `/aide/tickets/${id}`} />
    </div>
  );
}
