"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { api, ApiError, API_BASE } from "@/lib/api";
import { isSupervisor } from "@/lib/labels";
import { useErrorText, useFormat, useLabels, useLocale, useMessages } from "@/lib/i18n/client";
import { format, plural } from "@/lib/i18n/define";
import { common } from "@/lib/i18n/messages/common";
import { ticketWorkspace } from "@/lib/i18n/messages/ticketWorkspace";
import { useApi } from "@/lib/useApi";
import type {
  Agent,
  Category,
  Message,
  Section,
  TicketAction,
  TicketContext,
  TicketDetail,
  TicketPriority,
  TicketStatus,
} from "@/lib/types";
import { Conversation } from "@/components/Conversation";
import { AiSuggestionCard, MaplenouActions } from "@/components/TicketTools";
import { ticketTools } from "@/lib/i18n/messages/ticketTools";
import { priorityCodes, useVocabulary } from "@/lib/vocabulary";
import { History } from "@/components/History";
import { useSession } from "@/components/session";
import { TopBar } from "@/components/SupportShell";
import {
  Alert,
  ConfirmDialog,
  ErrorState,
  Loading,
  PriorityBadge,
  SlaBadge,
  StatusBadge,
  useToast,
} from "@/components/ui";

type Tab = "conversation" | "notes" | "history" | "attachments";

/** B4 — Support ticket detail: context, conversation, internal notes, history, actions and SLA. */
export default function SupportTicketPage() {
  const { id } = useParams<{ id: string }>();
  const session = useSession();
  const tx = useMessages(ticketWorkspace);
  const f = useFormat();
  const ticket = useApi<TicketDetail>(`/tickets/${id}`);
  const ai = useApi<{ available: boolean }>("/ai/status");
  const tt = useMessages(ticketTools);
  const locale = useLocale();
  const [tab, setTab] = useState<Tab>("conversation");
  const [refresh, setRefresh] = useState(0);

  const reload = async () => {
    await ticket.reload();
    setRefresh((r) => r + 1);
  };

  if (ticket.loading && !ticket.data) {
    return (
      <>
        <TopBar title={tx.ticket} />
        <div className="support-content"><Loading lines={6} /></div>
      </>
    );
  }
  if (ticket.error || !ticket.data) {
    return (
      <>
        <TopBar title={tx.ticket} />
        <div className="support-content">
          <Link href="/support/tickets" className="back-link">{tx.backToQueue}</Link>
          {ticket.error && <ErrorState error={ticket.error} onRetry={ticket.reload} />}
        </div>
      </>
    );
  }
  const t = ticket.data;
  const can = (a: TicketAction) => t.allowedActions.includes(a);
  const aiAvailable = !!ai.data?.available && can("USE_AI_ASSIST");
  const categoryName = (locale === "en" && t.category.nameEn) || t.category.name;
  const readOnly = !can("SEND_MESSAGE") && !can("ADD_INTERNAL_NOTE") && t.status !== "CLOSED";

  return (
    <>
      <TopBar
        title={
          <span className="detail-header">
            <Link href="/support/tickets" className="muted" style={{ fontWeight: 500, textDecoration: "none" }}>{tx.breadcrumbTickets}</Link>
            <span className="muted" aria-hidden="true">/</span>
            <span className="ticket-number" style={{ fontSize: "1rem" }}>#{t.ticketNumber}</span>
            {t.legacyNumber && <span className="xsmall muted mono" title={t.legacyNumber}>({t.legacyNumber})</span>}
            <StatusBadge status={t.status} kind={t.statusKind} />
            <PriorityBadge priority={t.priority} />
            {t.satisfaction && <span className="badge tone-neutral">★ {format(tt.ratedBy, { rating: t.satisfaction.rating })}</span>}
            {t.escalated && <span className="badge tone-danger">{tx.escalated}</span>}
          </span>
        }
      />
      <div className="support-content">
        <div>
          <h2 style={{ fontSize: "1.25rem" }}>{t.subject}</h2>
          <p className="small muted">
            {format(tx.createdMeta, { category: categoryName, date: f.dateTime(t.createdAt), requester: t.requester.name })}
            {t.requesterReportedUrgent && <span className="badge tone-serious" style={{ marginLeft: 8 }}>{tx.reportedUrgent}</span>}
          </p>
        </div>
        {readOnly && (
          <Alert tone="info" title={tx.readOnlyTitle}>
            {format(tx.readOnlyBody, { agent: t.assignedAgent?.name ?? tx.anotherAgent })}
          </Alert>
        )}
        {t.pendingReopenRequest && can("DECIDE_REOPEN") && <ReopenDecision ticket={t} onDone={reload} />}

        <div className="detail-layout">
          <div className="context-col stack">
            <ContextPanels ticketId={t.id} />
          </div>

          <section className="card" aria-label={tx.exchanges}>
            <div className="tablist" role="tablist">
              {(
                [
                  ["conversation", tx.tabConversation],
                  ["notes", tx.tabNotes],
                  ["history", tx.tabHistory],
                  ["attachments", tx.tabAttachments],
                ] as [Tab, string][]
              ).map(([key, label]) => (
                <button key={key} type="button" role="tab" id={`tab-${key}`} aria-selected={tab === key}
                        aria-controls={`panel-${key}`} onClick={() => setTab(key)}>
                  {label}
                </button>
              ))}
            </div>
            <div role="tabpanel" id={`panel-${tab}`} aria-labelledby={`tab-${tab}`}>
              {tab === "conversation" && (
                <Conversation ticketId={t.id} currentUserId={session.id} visibility="PUBLIC" canPost={can("SEND_MESSAGE")}
                              onPosted={reload} placeholder={tx.replyPlaceholder} agentTools={{ aiAvailable }} />
              )}
              {tab === "notes" && (
                <Conversation ticketId={t.id} currentUserId={session.id} visibility="INTERNAL" canPost={can("ADD_INTERNAL_NOTE")}
                              onPosted={reload} />
              )}
              {tab === "history" && <History ticketId={t.id} refreshKey={refresh} />}
              {tab === "attachments" && <AttachmentList ticketId={t.id} refreshKey={refresh} />}
            </div>
          </section>

          <div className="actions-col stack">
            <Actions ticket={t} onChanged={reload} />
            <AiSuggestionCard ticket={t} onChanged={reload} />
            <MaplenouActions ticket={t} onChanged={reload} />
            {t.sla && <SlaPanel ticket={t} />}
          </div>
        </div>
      </div>
    </>
  );
}

// ------------------------------------------------------------------ context (FR-011)

function SectionCard<T>({ title, section, children, emptyLabel }: {
  title: string;
  section: Section<T>;
  children: (data: T) => React.ReactNode;
  emptyLabel?: string;
}) {
  const tx = useMessages(ticketWorkspace);
  if (!section.linked) return null;
  return (
    <section className="card card-tight">
      <h3 className="panel-title">{title}</h3>
      {!section.available ? (
        <p className="small" style={{ color: "var(--warning)" }}>{tx.serviceUnavailable}</p>
      ) : section.data ? (
        children(section.data)
      ) : (
        <p className="small muted">{emptyLabel ?? tx.referenceNotFound}</p>
      )}
    </section>
  );
}

function ContextPanels({ ticketId }: { ticketId: string }) {
  const tx = useMessages(ticketWorkspace);
  const tc = useMessages(common);
  const labels = useLabels();
  const f = useFormat();
  const ctx = useApi<TicketContext>(`/tickets/${ticketId}/context`);
  if (ctx.loading && !ctx.data) return <div className="card"><Loading /></div>;
  if (ctx.error) return <ErrorState error={ctx.error} onRetry={ctx.reload} />;
  const c = ctx.data!;
  return (
    <>
      <section className="card card-tight">
        <h3 className="panel-title">{tx.requesterInfo}</h3>
        <div style={{ fontWeight: 700 }}>{c.requester.name}</div>
        <div className="xsmall muted" style={{ marginBottom: 8 }}>{c.requester.email ?? tc.none}</div>
        <dl className="kv">
          <dt>{tx.profile}</dt><dd>{labels.role[c.requester.role]}</dd>
          <dt>{tx.phone}</dt><dd>{c.requester.phone ?? tc.none}</dd>
          <dt>{tx.knownSince}</dt><dd>{f.date(c.requester.knownSince)}</dd>
          <dt>{tx.tickets}</dt><dd>{c.requester.ticketCount}</dd>
        </dl>
      </section>
      <SectionCard title={tx.order} section={c.order}>
        {(o) => (
          <dl className="kv">
            <dt>{tx.number}</dt><dd className="mono">{o.ref}</dd>
            <dt>{tx.date}</dt><dd>{f.date(o.orderedAt)}</dd>
            <dt>{tx.amount}</dt><dd>{f.money(o.amount, o.currency)}</dd>
            <dt>{tx.status}</dt><dd>{o.status}</dd>
            <dt>{tx.seller}</dt><dd>{o.sellerName}</dd>
          </dl>
        )}
      </SectionCard>
      <SectionCard title={tx.payment} section={c.payment}>
        {(p) => (
          <dl className="kv">
            <dt>{tx.number}</dt><dd className="mono">{p.ref}</dd>
            <dt>{tx.status}</dt><dd>{p.status}</dd>
            <dt>{tx.provider}</dt><dd>{p.provider}</dd>
            <dt>{tx.commission}</dt><dd>{f.money(p.commissionAmount, p.currency)}</dd>
            <dt>{tx.payout}</dt><dd>{p.payoutStatus ?? tc.none} · {f.money(p.payoutAmount, p.currency)}</dd>
          </dl>
        )}
      </SectionCard>
      <SectionCard title={tx.delivery} section={c.delivery}>
        {(d) => (
          <dl className="kv">
            <dt>{tx.number}</dt><dd className="mono">{d.ref}</dd>
            <dt>{tx.driver}</dt><dd>{d.driverName}</dd>
            <dt>{tx.status}</dt><dd>{d.status}</dd>
            <dt>{tx.destination}</dt><dd>{d.destination}</dd>
            <dt>{tx.timeSlot}</dt><dd>{d.timeSlot ?? tc.none}</dd>
            <dt>{tx.lastUpdate}</dt><dd>{f.dateTime(d.lastUpdateAt)}</dd>
          </dl>
        )}
      </SectionCard>
      <SectionCard title={tx.seller} section={c.seller}>
        {(s) => (
          <dl className="kv">
            <dt>{tx.shop}</dt><dd>{s.shopName}</dd>
            <dt>{tx.account}</dt><dd>{s.accountStatus}</dd>
            <dt>{tx.balance}</dt><dd>{f.money(s.balance, s.currency)}</dd>
          </dl>
        )}
      </SectionCard>
      <SectionCard title={tx.refund} section={c.refund}>
        {(r) => (
          <dl className="kv">
            <dt>{tx.number}</dt><dd className="mono">{r.ref}</dd>
            <dt>{tx.status}</dt><dd>{r.status}</dd>
            <dt>{tx.amount}</dt><dd>{f.money(r.amount, r.currency)}</dd>
          </dl>
        )}
      </SectionCard>
    </>
  );
}

// ------------------------------------------------------------------ attachments tab

function AttachmentList({ ticketId, refreshKey }: { ticketId: string; refreshKey: number }) {
  const tx = useMessages(ticketWorkspace);
  const fmt = useFormat();
  const messages = useApi<Message[]>(`/tickets/${ticketId}/messages?r=${refreshKey}`);
  if (messages.loading && !messages.data) return <Loading />;
  if (messages.error) return <ErrorState error={messages.error} onRetry={messages.reload} />;
  const files = (messages.data ?? []).flatMap((m) => m.attachments.map((a) => ({ ...a, message: m })));
  if (!files.length) return <p className="muted small">{tx.noAttachments}</p>;
  return (
    <ul className="list-links">
      {files.map((f) => (
        <li key={f.id}>
          <a className="list-link" href={`${API_BASE}/tickets/${ticketId}/attachments/${f.id}`} download={f.filename}>
            <span className="icon-tile" aria-hidden="true">📎</span>
            <span className="grow">
              <strong>{f.filename}</strong>
              {f.message.visibility === "INTERNAL" && <span className="internal-label" style={{ marginLeft: 8 }}>{tx.internalNote}</span>}
              <br />
              <span className="xsmall muted">{fmt.bytes(f.sizeBytes)} · {f.message.author.name} · {fmt.dateTime(f.createdAt)}</span>
            </span>
          </a>
        </li>
      ))}
    </ul>
  );
}

// ------------------------------------------------------------------ actions

const BUILT_IN_TARGETS = ["IN_PROGRESS", "WAITING_FOR_CUSTOMER", "RESOLVED", "CLOSED"];

type Pending =
  | { kind: "status"; target: TicketStatus }
  | { kind: "priority"; target: TicketPriority }
  | { kind: "escalate" }
  | null;

function Actions({ ticket: t, onChanged }: { ticket: TicketDetail; onChanged: () => Promise<void> }) {
  const toast = useToast();
  const session = useSession();
  const tx = useMessages(ticketWorkspace);
  const labels = useLabels();
  const f = useFormat();
  const errorText = useErrorText();
  const can = (a: TicketAction) => t.allowedActions.includes(a);
  const locale = useLocale();
  const priorities = priorityCodes(useVocabulary()).reverse();
  const categories = useApi<Category[]>(can("CHANGE_CATEGORY") ? "/categories" : null);
  const agents = useApi<Agent[]>(can("REASSIGN") ? "/agents" : null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState<Pending>(null);
  const [comment, setComment] = useState("");

  async function run(label: string, fn: () => Promise<unknown>) {
    setBusy(true);
    setError(null);
    try {
      await fn();
      toast(label);
      setPending(null);
      setComment("");
      await onChanged();
    } catch (e) {
      const msg = e instanceof ApiError ? errorText(e) : tx.actionFailed;
      setError(msg);
      toast(msg, true);
    } finally {
      setBusy(false);
    }
  }

  const changeStatus = (target: TicketStatus) =>
    run(format(tx.statusToast, { status: labels.status[target] }), () => api.put(`/tickets/${t.id}/status`, { status: target, comment: comment || null }));

  const statusTargets = t.allowedStatusTargets;
  const anything =
    can("ASSIGN_SELF") || can("REASSIGN") || statusTargets.length > 0 || can("CHANGE_PRIORITY") || can("CHANGE_CATEGORY") || can("ESCALATE");

  return (
    <section className="card stack-sm" aria-labelledby="actions-title">
      <h3 id="actions-title" className="panel-title">{tx.actionsTitle}</h3>
      {error && <p className="error small" role="alert">{error}</p>}

      <div className="small">
        <span className="muted">{tx.assignedTo}</span>
        <strong>{t.assignedAgent?.name ?? tx.unassigned}</strong>
      </div>
      {can("ASSIGN_SELF") && (
        <button type="button" className="btn btn-accent btn-sm" disabled={busy}
                onClick={() => run(tx.takenToast, () => api.put(`/tickets/${t.id}/assign`, {}))}>
          {t.assignedAgent ? tx.assignToMe : tx.takeOver}
        </button>
      )}
      {can("REASSIGN") && (
        <div className="field">
          <label htmlFor="reassign" className="small">{tx.assignToAgent}</label>
          <select id="reassign" className="select" value="" disabled={busy}
                  onChange={(e) => e.target.value && run(tx.assignedToast, () => api.put(`/tickets/${t.id}/assign`, { agentId: e.target.value }))}>
            <option value="">{tx.chooseAgent}</option>
            {agents.data?.filter((a) => a.id !== t.assignedAgent?.id).map((a) => (
              <option key={a.id} value={a.id}>{format(tx.agentOption, { name: a.name, count: a.openTickets })}</option>
            ))}
          </select>
        </div>
      )}

      {statusTargets.length > 0 && (
        <div className="stack-sm">
          <hr className="divider" />
          <span className="small muted">{tx.changeStatus}</span>
          <div className="row">
            {statusTargets.includes("IN_PROGRESS") && (
              <button type="button" className="btn btn-secondary btn-sm" disabled={busy} onClick={() => changeStatus("IN_PROGRESS")}>
                {t.statusKind === "WAITING_REQUESTER" || t.statusKind === "ON_HOLD" || t.status === "WAITING_FOR_CUSTOMER" ? tx.resumeWork : tx.startWork}
              </button>
            )}
            {statusTargets.includes("WAITING_FOR_CUSTOMER") && (
              <button type="button" className="btn btn-secondary btn-sm" disabled={busy} onClick={() => changeStatus("WAITING_FOR_CUSTOMER")}>
                {tx.requestInfo}
              </button>
            )}
            {statusTargets.includes("RESOLVED") && (
              <button type="button" className="btn btn-primary btn-sm" disabled={busy} onClick={() => setPending({ kind: "status", target: "RESOLVED" })}>
                {tx.markResolved}
              </button>
            )}
            {statusTargets.includes("CLOSED") && (
              <button type="button" className="btn btn-secondary btn-sm" disabled={busy} onClick={() => setPending({ kind: "status", target: "CLOSED" })}>
                {tx.closeTicket}
              </button>
            )}
            {/* statuses added by the administrators (DEC-CS11-02) */}
            {statusTargets.filter((s) => !BUILT_IN_TARGETS.includes(s)).map((s) => (
              <button key={s} type="button" className="btn btn-secondary btn-sm" disabled={busy} onClick={() => changeStatus(s)}>
                {labels.status[s]}
              </button>
            ))}
          </div>
        </div>
      )}

      {can("CHANGE_PRIORITY") && (
        <div className="field">
          <label htmlFor="priority" className="small">{tx.priority}</label>
          <select id="priority" className="select" value={t.priority} disabled={busy}
                  onChange={(e) => {
                    const p = e.target.value as TicketPriority;
                    if (p === "CRITICAL") setPending({ kind: "priority", target: p });
                    else run(format(tx.priorityToast, { priority: labels.priority[p] }), () => api.put(`/tickets/${t.id}/priority`, { priority: p }));
                  }}>
            {priorities
              .filter((p) => p !== "CRITICAL" || can("SET_CRITICAL"))
              .map((p) => <option key={p} value={p}>{labels.priority[p]}</option>)}
          </select>
        </div>
      )}
      {can("CHANGE_CATEGORY") && (
        <div className="field">
          <label htmlFor="category" className="small">{tx.category}</label>
          <select id="category" className="select" value={t.category.code} disabled={busy}
                  onChange={(e) => run(tx.categoryToast, () => api.put(`/tickets/${t.id}/category`, { categoryCode: e.target.value }))}>
            {(categories.data ?? [t.category]).map((c) => <option key={c.code} value={c.code}>{(locale === "en" && c.nameEn) || c.name}</option>)}
          </select>
        </div>
      )}
      {can("ESCALATE") && (
        <button type="button" className="btn btn-danger btn-sm" disabled={busy} onClick={() => setPending({ kind: "escalate" })}>
          {tx.escalateToManager}
        </button>
      )}
      {!anything && (
        <p className="small muted">
          {t.status === "CLOSED" ? tx.closedNoAction : tx.noActionForRole}
        </p>
      )}
      {isSupervisor(session.role) && t.escalated && <p className="xsmall muted">{format(tx.escalatedOn, { date: f.dateTime(t.escalatedAt) })}</p>}

      <ConfirmDialog
        open={pending?.kind === "status"}
        title={pending?.kind === "status" && pending.target === "CLOSED" ? tx.confirmCloseTitle : tx.confirmResolveTitle}
        confirmLabel={pending?.kind === "status" && pending.target === "CLOSED" ? tx.confirmClose : tx.confirmResolve}
        busy={busy}
        onCancel={() => setPending(null)}
        onConfirm={() => pending?.kind === "status" && changeStatus(pending.target)}
      >
        <p className="small secondary">
          {pending?.kind === "status" && pending.target === "CLOSED"
            ? tx.confirmCloseBody
            : tx.confirmResolveBody}
        </p>
        <div className="field">
          <label htmlFor="status-comment" className="small">{tx.statusComment}</label>
          <input id="status-comment" className="input" value={comment} maxLength={500} onChange={(e) => setComment(e.target.value)} />
        </div>
      </ConfirmDialog>

      <ConfirmDialog
        open={pending?.kind === "priority"}
        title={tx.confirmCriticalTitle}
        confirmLabel={tx.confirmCritical}
        danger
        busy={busy}
        onCancel={() => setPending(null)}
        onConfirm={() => run(format(tx.priorityToast, { priority: labels.priority.CRITICAL }), () => api.put(`/tickets/${t.id}/priority`, { priority: "CRITICAL", reason: comment || null }))}
      >
        <p className="small secondary">{tx.criticalBody}</p>
        <div className="field">
          <label htmlFor="critical-reason" className="small">{tx.reason}</label>
          <input id="critical-reason" className="input" value={comment} maxLength={500} onChange={(e) => setComment(e.target.value)}
                 placeholder={tx.criticalReasonPlaceholder} />
        </div>
      </ConfirmDialog>

      <ConfirmDialog
        open={pending?.kind === "escalate"}
        title={tx.confirmEscalateTitle}
        confirmLabel={tx.confirmEscalate}
        danger
        busy={busy}
        confirmDisabled={!comment.trim()}
        onCancel={() => setPending(null)}
        onConfirm={() => run(tx.escalatedToast, () => api.post(`/tickets/${t.id}/escalate`, { reason: comment.trim() }))}
      >
        <div className="field">
          <label htmlFor="escalate-reason" className="small required">{tx.escalateReason}</label>
          <textarea id="escalate-reason" className="textarea" value={comment} maxLength={500} onChange={(e) => setComment(e.target.value)} />
        </div>
      </ConfirmDialog>
    </section>
  );
}

// ------------------------------------------------------------------ reopening decision (BR-050, BR-051)

function ReopenDecision({ ticket: t, onDone }: { ticket: TicketDetail; onDone: () => Promise<void> }) {
  const toast = useToast();
  const tx = useMessages(ticketWorkspace);
  const f = useFormat();
  const errorText = useErrorText();
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const r = t.pendingReopenRequest!;

  async function decide(decision: "APPROVE" | "REJECT") {
    setBusy(true);
    setError(null);
    try {
      await api.put(`/tickets/${t.id}/reopen-requests/${r.id}`, { decision, note: note || null });
      toast(decision === "APPROVE" ? tx.reopenedToast : tx.reopenRejectedToast);
      await onDone();
    } catch (e) {
      setError(e instanceof ApiError ? errorText(e) : tx.decisionFailed);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="alert alert-warning" role="region" aria-label={tx.reopenRequest}>
      <div className="grow stack-sm">
        <strong>{format(tx.reopenRequestOn, { date: f.dateTime(r.requestedAt) })}</strong>
        <span className="pre-wrap">{format(tx.quoted, { text: r.reason })}</span>
        <label htmlFor="reopen-note" className="small">{tx.decisionNote}</label>
        <input id="reopen-note" className="input" value={note} maxLength={1000} onChange={(e) => setNote(e.target.value)} />
        {error && <span className="error small" role="alert">{error}</span>}
        <div className="row">
          <button type="button" className="btn btn-primary btn-sm" disabled={busy} onClick={() => decide("APPROVE")}>{tx.reopen}</button>
          <button type="button" className="btn btn-secondary btn-sm" disabled={busy} onClick={() => decide("REJECT")}>{tx.reject}</button>
        </div>
      </div>
    </div>
  );
}

// ------------------------------------------------------------------ SLA (FR-016, AC-SB-013)

function SlaPanel({ ticket: t }: { ticket: TicketDetail }) {
  const tx = useMessages(ticketWorkspace);
  const labels = useLabels();
  const f = useFormat();
  const locale = useLocale();
  const sla = t.sla!;
  return (
    <section className="card stack-sm" aria-labelledby="sla-title">
      <h3 id="sla-title" className="panel-title">{tx.sla}</h3>
      <div className="sla-row">
        <span>{tx.firstResponse}</span>
        <SlaBadge state={sla.firstResponse.state} />
      </div>
      <div className="xsmall muted">
        {format(tx.dueAt, { date: f.dateTime(sla.firstResponse.dueAt) })}
        {sla.firstResponse.completedAt && format(tx.answeredAt, { date: f.dateTime(sla.firstResponse.completedAt) })}
      </div>
      <div className="sla-row">
        <span>{tx.resolution}</span>
        <SlaBadge state={sla.resolution.state} />
      </div>
      <div className="xsmall muted">
        {format(tx.dueAt, { date: f.dateTime(sla.resolution.dueAt) })}
        {sla.resolution.completedAt && format(tx.resolvedAt, { date: f.dateTime(sla.resolution.completedAt) })}
      </div>
      {sla.periods.length > 1 && (
        <details className="small">
          <summary>{plural(locale, sla.periods.length, tx.periodsOne, tx.periodsOther)}</summary>
          <ul className="xsmall" style={{ paddingLeft: 16 }}>
            {sla.periods.map((p) => (
              <li key={p.periodNumber}>
                {format(tx.period, {
                  number: p.periodNumber,
                  priority: labels.priority[p.priority],
                  start: f.dateTime(p.startedAt),
                  due: f.dateTime(p.dueAt),
                })}
                {p.endedAt ? format(tx.periodEnded, { date: f.dateTime(p.endedAt) }) : tx.periodOngoing}
              </li>
            ))}
          </ul>
        </details>
      )}
      {sla.pausedSince && (
        <p className="xsmall" style={{ color: "var(--info)" }} role="status">
          {format(tx.pausedSince, { date: f.dateTime(sla.pausedSince) })}
        </p>
      )}
      <p className="xsmall muted">{tx.businessHours}</p>
    </section>
  );
}
