"use client";

import { useState } from "react";
import { api, ApiError, API_BASE, clientId } from "@/lib/api";
import { useApi } from "@/lib/useApi";
import type { Message, Visibility } from "@/lib/types";
import { isSupportRole } from "@/lib/labels";
import { useErrorText, useFormat, useLabels, useMessages } from "@/lib/i18n/client";
import { conversationMessages } from "@/lib/i18n/messages/conversation";
import { FilePicker } from "./FilePicker";
import { EmptyState, ErrorState, Loading, useToast } from "./ui";
import { ReplyHelpers } from "./TicketTools";

/**
 * Conversation (customer-visible messages) or internal notes. The two are separate views fed by separate
 * API calls, so an internal note can never be rendered in the requester's conversation (AC-UX-006, BR-016).
 */
export function Conversation({
  ticketId,
  currentUserId,
  visibility,
  canPost,
  onPosted,
  placeholder,
  agentTools,
}: {
  ticketId: string;
  currentUserId: string;
  visibility: Visibility;
  canPost: boolean;
  onPosted?: () => void;
  placeholder?: string;
  /** Support staff writing a public reply: canned replies and, when available, the assistant's draft. */
  agentTools?: { aiAvailable: boolean };
}) {
  const messages = useApi<Message[]>(`/tickets/${ticketId}/messages?visibility=${visibility}`);
  const internal = visibility === "INTERNAL";
  const t = useMessages(conversationMessages);

  return (
    <div className="stack">
      {messages.loading && !messages.data && <Loading />}
      {messages.error && <ErrorState error={messages.error} onRetry={messages.reload} />}
      {messages.data && messages.data.length === 0 && (
        <EmptyState icon={internal ? "🗒" : "💬"} title={internal ? t.noInternalNotes : t.noMessages}>
          {internal ? t.internalNotesNeverVisible : null}
        </EmptyState>
      )}
      {messages.data && messages.data.length > 0 && (
        <ol className="thread" aria-label={internal ? t.internalNotes : t.conversation} style={{ listStyle: "none", margin: 0, padding: 0 }}>
          {messages.data.map((m) => (
            <MessageItem key={m.id} message={m} ticketId={ticketId} mine={m.author.id === currentUserId} />
          ))}
        </ol>
      )}
      {internal && <p className="xsmall muted">{t.notVisibleToCustomer}</p>}
      {canPost && (
        <Composer
          ticketId={ticketId}
          internal={internal}
          placeholder={placeholder}
          agentTools={internal ? undefined : agentTools}
          onPosted={async () => {
            await messages.reload();
            onPosted?.();
          }}
        />
      )}
    </div>
  );
}

function MessageItem({ message: m, ticketId, mine }: { message: Message; ticketId: string; mine: boolean }) {
  const t = useMessages(conversationMessages);
  const labels = useLabels();
  const f = useFormat();
  const internal = m.visibility === "INTERNAL";
  const support = isSupportRole(m.author.role);
  const cls = internal ? "msg internal" : `msg${mine ? " mine" : support ? " support-msg" : ""}`;
  return (
    <li className={cls}>
      <div className="msg-meta">
        {internal && <span className="internal-label">{t.internalNote}</span>}
        <strong>{m.author.name}</strong>
        {m.author.role && <span className="badge tone-neutral">{labels.role[m.author.role]}</span>}
        <time dateTime={m.createdAt}>{f.dateTime(m.createdAt)}</time>
      </div>
      <div className="msg-bubble pre-wrap">{m.body}</div>
      {m.attachments.length > 0 && (
        <ul className="file-chips" style={{ listStyle: "none", margin: 0, padding: 0 }}>
          {m.attachments.map((a) => (
            <li key={a.id}>
              <a className="file-chip" href={`${API_BASE}/tickets/${ticketId}/attachments/${a.id}`} download={a.filename}>
                <span aria-hidden="true">📎</span> {a.filename} <span className="muted">{f.bytes(a.sizeBytes)}</span>
              </a>
            </li>
          ))}
        </ul>
      )}
    </li>
  );
}

function Composer({
  ticketId,
  internal,
  placeholder,
  onPosted,
  agentTools,
}: {
  ticketId: string;
  internal: boolean;
  placeholder?: string;
  onPosted: () => Promise<void> | void;
  agentTools?: { aiAvailable: boolean };
}) {
  const t = useMessages(conversationMessages);
  const errorText = useErrorText();
  const toast = useToast();
  const [body, setBody] = useState("");
  const [files, setFiles] = useState<File[]>([]);
  const [fileError, setFileError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  // Same id for retries of the same draft → the server never stores it twice (AC-SB-006).
  const [draftId, setDraftId] = useState(clientId);

  async function send(e: React.FormEvent) {
    e.preventDefault();
    if (!body.trim() || sending) return;
    setSending(true);
    setError(null);
    const path = `/tickets/${ticketId}/${internal ? "internal-notes" : "messages"}`;
    const payload = { body: body.trim(), clientMessageId: draftId };
    try {
      if (files.length) await api.multipart(path, "message", payload, files);
      else await api.post(path, payload);
      setBody("");
      setFiles([]);
      setDraftId(clientId());
      toast(internal ? t.noteAdded : t.messageSent);
      await onPosted();
    } catch (err) {
      setError(err instanceof ApiError ? errorText(err) : t.sendFailed);
    } finally {
      setSending(false);
    }
  }

  const label = internal ? t.addInternalNote : t.yourMessage;
  return (
    <form className={`composer${internal ? " internal-composer" : ""}`} onSubmit={send}>
      <label className="sr-only" htmlFor={`composer-${internal ? "note" : "msg"}`}>
        {label}
      </label>
      <textarea
        id={`composer-${internal ? "note" : "msg"}`}
        className="textarea"
        value={body}
        maxLength={5000}
        placeholder={placeholder ?? (internal ? t.notePlaceholder : t.messagePlaceholder)}
        onChange={(e) => setBody(e.target.value)}
        disabled={sending}
        rows={3}
      />
      {agentTools && (
        <ReplyHelpers ticketId={ticketId} aiAvailable={agentTools.aiAvailable} disabled={sending}
                      onInsert={(text) => setBody((prev) => (prev.trim() ? `${prev.trimEnd()}\n\n${text}` : text))} />
      )}
      {fileError && <p className="small" role="alert" style={{ color: "var(--danger)" }}>{fileError}</p>}
      {error && (
        <div className="alert alert-danger" role="alert">
          <span className="grow">{error}</span>
          <button type="submit" className="btn btn-secondary btn-sm">
            {t.retry}
          </button>
        </div>
      )}
      <div className="row-between">
        <FilePicker files={files} onChange={setFiles} onError={setFileError} disabled={sending} compact />
        <button type="submit" className={`btn ${internal ? "btn-secondary" : "btn-primary"}`} disabled={sending || !body.trim()}>
          {sending ? t.sending : internal ? t.addNote : t.send}
        </button>
      </div>
    </form>
  );
}
