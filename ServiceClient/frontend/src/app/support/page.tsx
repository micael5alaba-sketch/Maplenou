"use client";

import Link from "next/link";
import { TopBar } from "@/components/SupportShell";
import { BarList } from "@/components/Charts";
import { ErrorState, Loading, PriorityBadge, SlaBadge, StatusBadge } from "@/components/ui";
import { useFormat, useLabels, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { dashboard } from "@/lib/i18n/messages/dashboard";
import { useApi } from "@/lib/useApi";
import type { Dashboard } from "@/lib/types";
import { priorityCodes, useVocabulary } from "@/lib/vocabulary";

/** B1 — Support dashboard: operational workload (AC-WF-009). */
export default function DashboardPage() {
  const t = useMessages(dashboard);
  const d = useApi<Dashboard>("/dashboard");
  return (
    <>
      <TopBar title={t.title}>
        <Link href="/support/tickets" className="btn btn-secondary btn-sm">{t.openQueue}</Link>
      </TopBar>
      <div className="support-content">
        {d.loading && !d.data && <Loading lines={6} />}
        {d.error && <ErrorState error={d.error} onRetry={d.reload} />}
        {d.data && <DashboardBody d={d.data} />}
      </div>
    </>
  );
}

function Kpi({ label, value, note, href, tone }: { label: string; value: number | string; note?: string; href: string; tone?: string }) {
  return (
    <Link href={href} className="kpi">
      <div className="eyebrow">{label}</div>
      <div className="kpi-value" style={tone ? { color: tone } : undefined}>{value}</div>
      {note && <div className="kpi-note muted">{note}</div>}
    </Link>
  );
}

function DashboardBody({ d }: { d: Dashboard }) {
  const t = useMessages(dashboard);
  const labels = useLabels();
  const f = useFormat();
  const priorities = priorityCodes(useVocabulary());
  const csat = d.satisfactionLast30Days;
  return (
    <>
      <section className="kpis" aria-label={t.kpis}>
        <Kpi label={t.openTickets} value={d.openCount} note={format(t.newCount, { count: d.newCount })} href="/support/tickets?open=true" />
        <Kpi label={t.unassigned} value={d.unassignedCount} note={t.toPickUp} href="/support/tickets?assignee=unassigned&open=true" />
        <Kpi label={t.inProgress} value={d.inProgressCount} href="/support/tickets?status=IN_PROGRESS" />
        <Kpi label={t.waitingForCustomer} value={d.waitingForCustomerCount} href="/support/tickets?status=WAITING_FOR_CUSTOMER" />
        <Kpi label={t.resolvedToday} value={d.resolvedTodayCount} href="/support/tickets?status=RESOLVED" />
        <Kpi label={t.slaBreached} value={d.slaBreachedOpenCount} note={format(t.escalatedCount, { count: d.escalatedOpenCount })}
             href="/support/tickets?slaBreached=true" tone={d.slaBreachedOpenCount ? "var(--danger)" : undefined} />
        <Kpi label={t.satisfaction}
             value={csat?.average != null ? `${f.number(Math.round(csat.average * 10) / 10)} / 5` : "—"}
             note={csat?.count ? format(t.satisfactionNote, { count: csat.count, percent: Math.round(csat.satisfiedPercent ?? 0) }) : t.satisfactionNone}
             href="/support/rapports" />
      </section>

      <div className="grid-3">
        <section className="card" aria-labelledby="by-priority">
          <h2 id="by-priority">{t.openByPriority}</h2>
          <BarList label={t.openByPriority}
                   items={priorities.map((p) => ({ key: p, label: labels.priority[p], value: d.openByPriority[p] ?? 0 }))} />
        </section>
        <section className="card" aria-labelledby="by-category">
          <h2 id="by-category">{t.openByCategory}</h2>
          {d.openByCategory.length === 0 ? <p className="muted small">{t.noOpenTickets}</p> : (
            <BarList label={t.openByCategory}
                     items={d.openByCategory.map((c) => ({ key: c.code, label: c.name, value: c.count }))} />
          )}
        </section>
        <section className="card stack-sm" aria-labelledby="recent">
          <div className="row-between">
            <h2 id="recent">{t.recentTickets}</h2>
            <Link href="/support/tickets" className="small">{t.seeAll}</Link>
          </div>
          {d.recent.length === 0 && <p className="muted small">{t.noOpenTickets}</p>}
          <ul className="list-links">
            {d.recent.map((ticket) => (
              <li key={ticket.id}>
                <Link href={`/support/tickets/${ticket.id}`} className="list-link" style={{ display: "block" }}>
                  <div className="row-between">
                    <span className="ticket-number">#{ticket.ticketNumber}</span>
                    <PriorityBadge priority={ticket.priority} />
                  </div>
                  <div style={{ fontWeight: 600, margin: "2px 0" }}>{ticket.subject}</div>
                  <div className="row xsmall muted">
                    {ticket.requester.name} · {ticket.requester.role ? labels.role[ticket.requester.role].toLowerCase() : ""} · {f.timeAgo(ticket.createdAt)}
                  </div>
                  <div className="row" style={{ marginTop: 4 }}>
                    <StatusBadge status={ticket.status} />
                    {ticket.slaState && ticket.slaState !== "ON_TRACK" && <SlaBadge state={ticket.slaState} />}
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </>
  );
}
