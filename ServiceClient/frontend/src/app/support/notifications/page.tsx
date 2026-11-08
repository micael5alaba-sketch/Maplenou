"use client";

import { NotificationList } from "@/components/NotificationList";
import { PushNotifications } from "@/components/PushNotifications";
import { TopBar } from "@/components/SupportShell";
import { useMessages } from "@/lib/i18n/client";
import { queue } from "@/lib/i18n/messages/queue";

export default function SupportNotificationsPage() {
  const t = useMessages(queue);
  return (
    <>
      <TopBar title={t.notificationsTitle} />
      <div className="support-content stack" style={{ maxWidth: 820 }}>
        <PushNotifications />
        <NotificationList ticketHref={(id) => `/support/tickets/${id}`} />
      </div>
    </>
  );
}
