"use client";

import Link from "next/link";
import { useState } from "react";
import { api } from "@/lib/api";
import { useFormat, useLocale, useMessages } from "@/lib/i18n/client";
import { localizeNotification } from "@/lib/i18n/messages/notifications";
import { notificationList } from "@/lib/i18n/messages/notificationList";
import { useApi } from "@/lib/useApi";
import type { Notification, Page } from "@/lib/types";
import { EmptyState, ErrorState, Loading, useToast } from "./ui";

/** C1 — Notifications. Push delivery is separate; this in-app list is always available. */
export function NotificationList({ ticketHref }: { ticketHref: (ticketId: string) => string }) {
  const t = useMessages(notificationList);
  const f = useFormat();
  const locale = useLocale();
  const toast = useToast();
  const [unreadOnly, setUnreadOnly] = useState(false);
  const list = useApi<Page<Notification>>(`/notifications?size=50&unreadOnly=${unreadOnly}`);

  async function markAll() {
    await api.post("/notifications/read-all");
    toast(t.allRead);
    await list.reload();
  }

  async function open(n: Notification) {
    if (!n.readAt) await api.post(`/notifications/${n.id}/read`).catch(() => undefined);
  }

  return (
    <div className="stack">
      <div className="row-between">
        <div className="tabs" role="tablist" aria-label={t.filterLabel}>
          <button type="button" role="tab" className="tab" aria-selected={!unreadOnly} onClick={() => setUnreadOnly(false)}>{t.all}</button>
          <button type="button" role="tab" className="tab" aria-selected={unreadOnly} onClick={() => setUnreadOnly(true)}>{t.unread}</button>
        </div>
        <button type="button" className="btn btn-secondary btn-sm" onClick={markAll}>{t.markAllRead}</button>
      </div>
      {list.loading && !list.data && <Loading />}
      {list.error && <ErrorState error={list.error} onRetry={list.reload} />}
      {list.data?.items.length === 0 && <EmptyState icon="🔔" title={unreadOnly ? t.emptyUnread : t.empty} />}
      <ul className="list-links">
        {list.data?.items.map((n) => {
          const text = localizeNotification(n, locale);
          const content = (
            <>
              <span className="icon-tile" aria-hidden="true">{n.readAt ? "✓" : "●"}</span>
              <span className="grow">
                <strong>{text.title}</strong>
                {!n.readAt && <span className="sr-only">{t.unreadSuffix}</span>}
                <br />
                <span className="small secondary">{text.body}</span>
                <br />
                <time className="xsmall muted" dateTime={n.createdAt}>{f.dateTime(n.createdAt)}</time>
              </span>
            </>
          );
          return (
            <li key={n.id}>
              {n.ticketId ? (
                <Link href={ticketHref(n.ticketId)} className="list-link" onClick={() => open(n)}
                      style={n.readAt ? undefined : { borderColor: "var(--brand)" }}>
                  {content}
                </Link>
              ) : (
                <div className="list-link">{content}</div>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
