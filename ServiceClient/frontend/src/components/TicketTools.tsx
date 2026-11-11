"use client";

import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { useErrorText, useFormat, useLabels, useLocale, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { ticketTools } from "@/lib/i18n/messages/ticketTools";
import { useApi } from "@/lib/useApi";
import type { AiDraft, CannedReply, Category, TicketAction, TicketContext, TicketDetail } from "@/lib/types";
import { ConfirmDialog, useToast } from "./ui";

// ------------------------------------------------------------------ composer helpers (agents)

/**
 * Canned replies and the assistant's draft for the public reply. Both only fill the text area: the agent reads,
 * edits and sends the message themselves.
 */
export function ReplyHelpers({ ticketId, aiAvailable, onInsert, disabled }: {
  ticketId: string;
  aiAvailable: boolean;
  onInsert: (text: string) => void;
  disabled?: boolean;
}) {
  const t = useMessages(ticketTools);
  const locale = useLocale();
  const errorText = useErrorText();
  const toast = useToast();
  const replies = useApi<CannedReply[]>("/canned-replies");
  const [drafting, setDrafting] = useState(false);
  const [sources, setSources] = useState<string[]>([]);
  const ordered = [...(replies.data ?? [])].sort((a, b) =>
    a.language === b.language ? a.title.localeCompare(b.title) : a.language === locale ? -1 : 1);

  async function draft() {
    setDrafting(true);
    try {
      const d = await api.post<AiDraft>(`/tickets/${ticketId}/ai/draft-reply`, { language: locale });
      onInsert(d.text);
      setSources(d.references);
      toast(t.aiDraftReady);
    } catch (e) {
      toast(e instanceof ApiError ? errorText(e) : String(e), true);
    } finally {
      setDrafting(false);
    }
  }

  if (!ordered.length && !aiAvailable) return null;
  return (
    <div className="row" style={{ flexWrap: "wrap", gap: 8 }}>
      {ordered.length > 0 && (
        <>
          <label className="sr-only" htmlFor={`canned-${ticketId}`}>{t.insertCanned}</label>
          <select id={`canned-${ticketId}`} className="select" value="" disabled={disabled}
                  onChange={(e) => {
                    const r = ordered.find((x) => x.id === e.target.value);
                    if (r) onInsert(r.body);
                  }}>
            <option value="">{t.cannedReplies}</option>
            {ordered.map((r) => <option key={r.id} value={r.id}>{r.title} ({r.language.toUpperCase()})</option>)}
          </select>
        </>
      )}
      {aiAvailable && (
        <button type="button" className="btn btn-secondary btn-sm" disabled={disabled || drafting} onClick={draft}>
          {drafting ? t.aiDrafting : `✨ ${t.aiDraft}`}
        </button>
      )}
      {sources.length > 0 && <span className="xsmall muted">{format(t.aiDraftSources, { titles: sources.join(", ") })}</span>}
    </div>
  );
}

// ------------------------------------------------------------------ AI suggestion (DEC-CS11-08)

export function AiSuggestionCard({ ticket: tk, onChanged }: { ticket: TicketDetail; onChanged: () => Promise<void> }) {
  const t = useMessages(ticketTools);
  const labels = useLabels();
  const f = useFormat();
  const locale = useLocale();
  const toast = useToast();
  const errorText = useErrorText();
  const categories = useApi<Category[]>("/categories");
  const [busy, setBusy] = useState(false);
  const s = tk.aiSuggestion;
  if (!s) return null;
  const cat = categories.data?.find((c) => c.code === s.categoryCode);
  const catName = (cat && ((locale === "en" && cat.nameEn) || cat.name)) || s.categoryCode;

  async function decide(apply: boolean) {
    setBusy(true);
    try {
      await api.post(`/tickets/${tk.id}/ai/suggestion`, { apply });
      toast(apply ? t.aiApplied : t.aiDismissed);
      await onChanged();
    } catch (e) {
      toast(e instanceof ApiError ? errorText(e) : String(e), true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card stack-sm" aria-labelledby="ai-title">
      <h3 id="ai-title" className="panel-title">✨ {t.aiTitle}</h3>
      <p className="small">{format(t.aiSuggests, { category: catName, priority: labels.priority[s.priorityCode] })}</p>
      {s.reason && <p className="xsmall secondary">{s.reason}</p>}
      <p className="xsmall muted">{format(t.aiConfidence, { percent: Math.round(s.confidence * 100) })}</p>
      {s.decision ? (
        <p className="xsmall muted">
          {format(t.aiDecided, {
            decision: s.decision === "APPLIED" ? t.aiDecisionApplied : t.aiDecisionDismissed,
            date: f.dateTime(s.createdAt),
          })}
        </p>
      ) : tk.allowedActions.includes("USE_AI_ASSIST") && (
        <div className="row">
          <button type="button" className="btn btn-primary btn-sm" disabled={busy} onClick={() => decide(true)}>{t.aiApply}</button>
          <button type="button" className="btn btn-tertiary btn-sm" disabled={busy} onClick={() => decide(false)}>{t.aiDismiss}</button>
        </div>
      )}
      <p className="xsmall muted">{t.aiNotice}</p>
    </section>
  );
}

// ------------------------------------------------------------------ actions on Maplenou (DEC-CS11-07)

type Kind = "refund" | "reschedule" | "driver";
const CONDITIONS = ["GOOD", "NEW", "DAMAGED"] as const;

export function MaplenouActions({ ticket: tk, onChanged }: { ticket: TicketDetail; onChanged: () => Promise<void> }) {
  const t = useMessages(ticketTools);
  const f = useFormat();
  const toast = useToast();
  const errorText = useErrorText();
  const can = (a: TicketAction) => tk.allowedActions.includes(a);
  const any = can("REQUEST_REFUND") || can("RESCHEDULE_DELIVERY") || can("CONTACT_DRIVER");
  const ctx = useApi<TicketContext>(any && can("REQUEST_REFUND") ? `/tickets/${tk.id}/context` : null);
  const [open, setOpen] = useState<Kind | null>(null);
  const [text, setText] = useState("");
  const [condition, setCondition] = useState<(typeof CONDITIONS)[number]>("GOOD");
  const [subOrderId, setSubOrderId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const log = tk.externalActions ?? [];
  if (!any && !log.length) return null;
  const subOrders = ctx.data?.order.data?.subOrders ?? [];

  function start(kind: Kind) {
    setOpen(kind);
    setText("");
    setError(null);
  }

  async function submit() {
    setBusy(true);
    setError(null);
    try {
      if (open === "refund") {
        await api.post(`/tickets/${tk.id}/actions/refund-request`,
          { reason: text.trim(), productCondition: condition, subOrderId: subOrderId || null });
      } else if (open === "reschedule") {
        await api.post(`/tickets/${tk.id}/actions/reschedule-delivery`, { reason: text.trim() });
      } else if (open === "driver") {
        await api.post(`/tickets/${tk.id}/actions/contact-driver`, { message: text.trim() });
      }
      toast(t.actionDone);
      setOpen(null);
    } catch (e) {
      setError(e instanceof ApiError ? errorText(e) : String(e));
    } finally {
      setBusy(false);
      // the outcome (accepted or refused) is recorded on the ticket either way
      await onChanged();
    }
  }

  const minLength = open === "refund" ? 10 : 1;
  const titles: Record<Kind, string> = { refund: t.refundTitle, reschedule: t.rescheduleTitle, driver: t.contactDriverTitle };
  return (
    <section className="card stack-sm" aria-labelledby="maplenou-title">
      <h3 id="maplenou-title" className="panel-title">{t.maplenouTitle}</h3>
      {can("REQUEST_REFUND") && (
        <button type="button" className="btn btn-secondary btn-sm" onClick={() => start("refund")}>{t.refund}</button>
      )}
      {can("RESCHEDULE_DELIVERY") && (
        <button type="button" className="btn btn-secondary btn-sm" onClick={() => start("reschedule")}>{t.reschedule}</button>
      )}
      {can("CONTACT_DRIVER") && (
        <button type="button" className="btn btn-secondary btn-sm" onClick={() => start("driver")}>{t.contactDriver}</button>
      )}
      {log.length > 0 && (
        <details className="small" open={log.length < 3}>
          <summary>{t.actionsLog}</summary>
          <ul className="xsmall" style={{ paddingLeft: 16 }}>
            {log.map((a) => (
              <li key={a.id}>
                {t[`action${a.type}`]} · {f.dateTime(a.createdAt)} · {a.requestedBy?.name ?? ""} —{" "}
                {a.succeeded ? <span style={{ color: "var(--success)" }}>{t.actionOk}{a.resultRef ? ` (${a.resultRef})` : ""}</span>
                  : <span style={{ color: "var(--danger)" }}>{format(t.actionRefused, { error: a.error ?? "" })}</span>}
              </li>
            ))}
          </ul>
        </details>
      )}

      <ConfirmDialog
        open={open !== null}
        title={open ? titles[open] : ""}
        confirmLabel={t.send}
        busy={busy}
        confirmDisabled={text.trim().length < minLength}
        onCancel={() => setOpen(null)}
        onConfirm={submit}
      >
        {open === "refund" && <p className="small secondary">{t.refundBody}</p>}
        {open === "reschedule" && <p className="small secondary">{t.rescheduleBody}</p>}
        {open === "refund" && subOrders.length > 1 && (
          <div className="field">
            <label htmlFor="sub-order" className="small required">{t.subOrder}</label>
            <select id="sub-order" className="select" value={subOrderId} onChange={(e) => setSubOrderId(e.target.value)}>
              <option value="" />
              {subOrders.map((s) => <option key={s.id} value={s.id}>{s.shopName} — {f.money(s.subtotal)} ({s.status})</option>)}
            </select>
          </div>
        )}
        {open === "refund" && (
          <div className="field">
            <label htmlFor="condition" className="small">{t.productCondition}</label>
            <select id="condition" className="select" value={condition}
                    onChange={(e) => setCondition(e.target.value as (typeof CONDITIONS)[number])}>
              {CONDITIONS.map((c) => <option key={c} value={c}>{t[`condition${c}`]}</option>)}
            </select>
          </div>
        )}
        <div className="field">
          <label htmlFor="action-text" className="small required">
            {open === "refund" ? t.refundReason : open === "driver" ? t.message : t.reason}
          </label>
          <textarea id="action-text" className="textarea" value={text} maxLength={open === "refund" ? 900 : open === "driver" ? 1000 : 500}
                    onChange={(e) => setText(e.target.value)} />
        </div>
        {error && <p className="error small" role="alert">{error}</p>}
      </ConfirmDialog>
    </section>
  );
}

// ------------------------------------------------------------------ satisfaction (requester, DEC-CS11-05)

export function SatisfactionCard({ ticket: tk, onRated }: { ticket: TicketDetail; onRated: () => Promise<void> }) {
  const t = useMessages(ticketTools);
  const f = useFormat();
  const toast = useToast();
  const errorText = useErrorText();
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (tk.satisfaction) {
    return (
      <p className="small muted" role="status">
        {format(t.ratedOn, { rating: tk.satisfaction.rating, date: f.date(tk.satisfaction.ratedAt) })}
      </p>
    );
  }
  if (!tk.allowedActions.includes("RATE")) return null;

  async function send(e: React.FormEvent) {
    e.preventDefault();
    if (!rating) return;
    setBusy(true);
    setError(null);
    try {
      await api.post(`/tickets/${tk.id}/satisfaction`, { rating, comment: comment.trim() || null });
      toast(t.rateThanks);
      await onRated();
    } catch (err) {
      setError(err instanceof ApiError ? errorText(err) : String(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="card stack-sm" onSubmit={send} aria-labelledby="rate-title">
      <h2 id="rate-title" style={{ fontSize: "1rem" }}>{t.rateTitle}</h2>
      <fieldset style={{ border: 0, padding: 0, margin: 0 }}>
        <legend className="small secondary">{t.rateHint}</legend>
        <div className="segmented" style={{ marginTop: 6 }}>
          {[1, 2, 3, 4, 5].map((n) => (
            <label key={n} title={format(t.rateStar, { n })}>
              <input type="radio" name="rating" value={n} checked={rating === n} onChange={() => setRating(n)}
                     aria-label={format(t.rateStar, { n })} />
              <span aria-hidden="true">{"★".repeat(n)}</span>
            </label>
          ))}
        </div>
      </fieldset>
      <div className="field">
        <label htmlFor="rate-comment" className="small">{t.rateComment}</label>
        <textarea id="rate-comment" className="textarea" rows={2} maxLength={1000} value={comment}
                  onChange={(e) => setComment(e.target.value)} />
      </div>
      {error && <p className="error small" role="alert">{error}</p>}
      <div>
        <button type="submit" className="btn btn-primary btn-sm" disabled={busy || !rating}>{t.rateSend}</button>
      </div>
    </form>
  );
}
