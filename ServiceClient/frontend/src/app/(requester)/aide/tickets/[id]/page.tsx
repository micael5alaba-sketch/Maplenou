"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { useErrorText, useFormat, useLocale, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { requesterTicket } from "@/lib/i18n/messages/requester";
import { useApi } from "@/lib/useApi";
import type { TicketDetail } from "@/lib/types";
import { Conversation } from "@/components/Conversation";
import { SatisfactionCard } from "@/components/TicketTools";
import { History } from "@/components/History";
import { useSession } from "@/components/session";
import { Alert, ErrorState, Loading, PriorityBadge, StatusBadge, useToast } from "@/components/ui";

/** A4 — Requester ticket detail. Shows no support-only content (AC-SI-004). */
export default function RequesterTicketPage() {
  const m = useMessages(requesterTicket);
  const f = useFormat();
  const locale = useLocale();
  const { id } = useParams<{ id: string }>();
  const session = useSession();
  const ticket = useApi<TicketDetail>(`/tickets/${id}`);
  const [refresh, setRefresh] = useState(0);

  const reload = async () => {
    await ticket.reload();
    setRefresh((r) => r + 1);
  };

  if (ticket.loading && !ticket.data) return <Loading lines={5} />;
  if (ticket.error) {
    return (
      <div className="stack">
        <Link href="/aide/tickets" className="back-link">{m.backToTickets}</Link>
        <ErrorState error={ticket.error} onRetry={ticket.reload} />
      </div>
    );
  }
  const t = ticket.data!;
  const can = (a: string) => t.allowedActions.includes(a as never);
  const kind = t.statusKind;

  return (
    <div className="stack">
      <Link href="/aide/tickets" className="back-link">{m.backToTickets}</Link>
      <header className="stack-sm">
        <span className="ticket-number">#{t.ticketNumber}</span>
        {t.legacyNumber && <span className="xsmall muted mono" title={t.legacyNumber}>({t.legacyNumber})</span>}
        <h1>{t.subject}</h1>
        <p className="xsmall muted">{format(m.createdOn, { date: f.dateTime(t.createdAt) })}</p>
      </header>

      <div className="card card-tight">
        <dl className="kv">
          <dt>{m.status}</dt><dd><StatusBadge status={t.status} kind={t.statusKind} /></dd>
          <dt>{m.priority}</dt><dd><PriorityBadge priority={t.priority} /></dd>
          <dt>{m.category}</dt><dd>{(locale === "en" && t.category.nameEn) || t.category.name}</dd>
          {t.references.orderRef && (<><dt>{m.linkedOrder}</dt><dd className="mono">{t.references.orderRef}</dd></>)}
          {t.references.deliveryRef && (<><dt>{m.delivery}</dt><dd className="mono">{t.references.deliveryRef}</dd></>)}
          <dt>{m.handledBy}</dt><dd>{t.assignedAgent?.name ?? m.awaitingAssignment}</dd>
        </dl>
      </div>

      {(kind === "WAITING_REQUESTER" || t.status === "WAITING_FOR_CUSTOMER") && (
        <Alert tone="warning" title={m.actionRequiredTitle}>
          {m.actionRequiredBody}
        </Alert>
      )}
      {(kind === "RESOLVED" || t.status === "RESOLVED") && <ReopenBlock ticket={t} onDone={reload} />}
      {(kind === "RESOLVED" || kind === "CLOSED" || t.status === "RESOLVED" || t.status === "CLOSED") && (
        <SatisfactionCard ticket={t} onRated={reload} />
      )}
      {(kind === "CLOSED" || t.status === "CLOSED") && (
        <Alert tone="info" title={m.closedTitle} action={<Link className="btn btn-secondary btn-sm" href="/aide/nouveau">{m.newTicket}</Link>}>
          {m.closedBody}
        </Alert>
      )}

      <section className="card stack" aria-labelledby="conv-title">
        <h2 id="conv-title">{m.conversation}</h2>
        <Conversation
          ticketId={t.id}
          currentUserId={session.id}
          visibility="PUBLIC"
          canPost={can("SEND_MESSAGE")}
          onPosted={reload}
          placeholder={t.status === "WAITING_FOR_CUSTOMER" ? m.replyPlaceholder : undefined}
        />
      </section>

      <section className="card" aria-labelledby="hist-title">
        <h2 id="hist-title">{m.tracking}</h2>
        <History ticketId={t.id} refreshKey={refresh} />
      </section>
    </div>
  );
}

function ReopenBlock({ ticket: t, onDone }: { ticket: TicketDetail; onDone: () => Promise<void> }) {
  const m = useMessages(requesterTicket);
  const f = useFormat();
  const errorText = useErrorText();
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  // The error itself is kept (not its text) so the message follows a language switch.
  const [error, setError] = useState<{ error: unknown } | null>(null);

  if (t.pendingReopenRequest) {
    return (
      <Alert tone="info" title={m.reopenPendingTitle}>
        {format(m.reopenPendingBody, { date: f.dateTime(t.pendingReopenRequest.requestedAt) })}
      </Alert>
    );
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!reason.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await api.post(`/tickets/${t.id}/reopen-requests`, { reason: reason.trim() });
      toast(m.reopenSent);
      await onDone();
    } catch (err) {
      setError({ error: err });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="stack-sm">
      <Alert tone="success" title={m.resolvedTitle}>
        {m.resolvedBody}
        {t.lastReopenDecision?.status === "REJECTED" && (
          <span className="small" style={{ display: "block", marginTop: 4 }}>
            {t.lastReopenDecision.decisionNote
              ? format(m.previousRejectedWithNote, { note: t.lastReopenDecision.decisionNote })
              : m.previousRejected}
          </span>
        )}
      </Alert>
      {t.allowedActions.includes("REQUEST_REOPEN") && !open && (
        <button type="button" className="btn btn-secondary" onClick={() => setOpen(true)}>
          {m.requestReopen}
        </button>
      )}
      {open && (
        <form className="card stack-sm" onSubmit={submit}>
          <label htmlFor="reopen-reason" className="label required">{m.reopenReason}</label>
          <textarea id="reopen-reason" className="textarea" value={reason} maxLength={1000} onChange={(e) => setReason(e.target.value)} />
          {error && <p className="error" role="alert">{error.error instanceof ApiError ? errorText(error.error) : m.reopenFailed}</p>}
          <div className="row">
            <button type="submit" className="btn btn-primary" disabled={busy || !reason.trim()}>
              {busy ? m.sending : m.sendRequest}
            </button>
            <button type="button" className="btn btn-tertiary" onClick={() => setOpen(false)}>{m.cancel}</button>
          </div>
        </form>
      )}
    </div>
  );
}
