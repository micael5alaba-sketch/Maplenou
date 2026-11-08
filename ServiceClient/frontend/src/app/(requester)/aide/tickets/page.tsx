"use client";

import Link from "next/link";
import { Suspense, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useApi } from "@/lib/useApi";
import { useFormat, useMessages } from "@/lib/i18n/client";
import { myTickets } from "@/lib/i18n/messages/requester";
import type { Page, TicketSummary } from "@/lib/types";
import { EmptyState, ErrorState, Loading, PriorityBadge, StatusBadge } from "@/components/ui";

type Filter = "tous" | "ouverts" | "fermes";

const QUERY: Record<Filter, string> = {
  tous: "",
  ouverts: "&open=true",
  fermes: "&status=RESOLVED&status=CLOSED",
};

/** A2 — My Tickets. */
function MyTickets() {
  const t = useMessages(myTickets);
  const f = useFormat();
  const params = useSearchParams();
  const initial = (params.get("filtre") as Filter) ?? "tous";
  const [filter, setFilter] = useState<Filter>(QUERY[initial] !== undefined ? initial : "tous");
  const [size, setSize] = useState(10);
  const list = useApi<Page<TicketSummary>>(`/tickets?size=${size}&sort=updatedAt,desc${QUERY[filter]}`);

  return (
    <div className="stack">
      <div className="row-between">
        <h1>{t.title}</h1>
        <Link href="/aide/nouveau" className="btn btn-primary btn-sm">{t.newTicket}</Link>
      </div>
      <div className="tabs" role="tablist" aria-label={t.filterLabel}>
        {(["tous", "ouverts", "fermes"] as Filter[]).map((key) => (
          <button key={key} type="button" role="tab" className="tab" aria-selected={filter === key} onClick={() => setFilter(key)}>
            {key === "tous" ? t.filterAll : key === "ouverts" ? t.filterOpen : t.filterClosed}
          </button>
        ))}
      </div>
      {list.loading && !list.data && <Loading />}
      {list.error && <ErrorState error={list.error} onRetry={list.reload} />}
      {list.data && list.data.items.length === 0 && (
        <EmptyState icon="📭" title={filter === "tous" ? t.emptyAll : t.emptyFiltered}>
          {filter === "tous" && <Link href="/aide/nouveau">{t.createTicket}</Link>}
        </EmptyState>
      )}
      <ul className="stack-sm" style={{ listStyle: "none", margin: 0, padding: 0 }}>
        {list.data?.items.map((ticket) => (
          <li key={ticket.id}>
            <Link href={`/aide/tickets/${ticket.id}`} className="ticket-card">
              <div className="row-between">
                <span className="ticket-number">#{ticket.ticketNumber}</span>
                <span className="xsmall muted">{f.date(ticket.updatedAt)}</span>
              </div>
              <div style={{ fontWeight: 600, margin: "4px 0 8px" }}>{ticket.subject}</div>
              <div className="row">
                <StatusBadge status={ticket.status} />
                <PriorityBadge priority={ticket.priority} />
                {(ticket.orderRef || ticket.deliveryRef) && <span className="xsmall muted mono">{ticket.orderRef ?? ticket.deliveryRef}</span>}
              </div>
            </Link>
          </li>
        ))}
      </ul>
      {list.data && list.data.totalItems > list.data.items.length && (
        <button type="button" className="btn btn-secondary" onClick={() => setSize(size + 10)} disabled={list.loading}>
          {list.loading ? t.loading : t.seeMore}
        </button>
      )}
    </div>
  );
}

export default function MyTicketsPage() {
  return (
    <Suspense fallback={<Loading />}>
      <MyTickets />
    </Suspense>
  );
}
