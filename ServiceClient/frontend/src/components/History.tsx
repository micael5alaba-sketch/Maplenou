"use client";

import { useFormat, useLabels, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { historyMessages } from "@/lib/i18n/messages/history";
import { useApi } from "@/lib/useApi";
import type { HistoryEntry } from "@/lib/types";
import { EmptyState, ErrorState, Loading } from "./ui";

export function History({ ticketId, refreshKey = 0 }: { ticketId: string; refreshKey?: number }) {
  const t = useMessages(historyMessages);
  const labels = useLabels();
  const f = useFormat();
  const history = useApi<HistoryEntry[]>(`/tickets/${ticketId}/history?r=${refreshKey}`);
  if (history.loading && !history.data) return <Loading />;
  if (history.error) return <ErrorState error={history.error} onRetry={history.reload} />;
  if (!history.data?.length) return <EmptyState title={t.empty} />;
  return (
    <ol className="timeline" aria-label={t.ariaLabel}>
      {history.data.map((h) => {
        const from = labels.historyValue(h.action, h.oldValue);
        const to = labels.historyValue(h.action, h.newValue);
        return (
          <li key={h.id}>
            <time dateTime={h.createdAt}>{f.dateTime(h.createdAt)}</time>
            <span>
              <strong>{labels.history[h.action] ?? h.action}</strong>
              {from && to ? format(t.change, { from, to }) : to ? format(t.value, { to }) : ""}
              {h.actor ? <span className="muted"> — {h.actor.name}</span> : null}
              {h.details ? <span className="muted small"> · {h.details}</span> : null}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
