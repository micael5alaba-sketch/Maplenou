"use client";

import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import type { Role, SlaState, StatusKind, TicketPriority, TicketStatus } from "@/lib/types";
import { kindOf, useVocabulary } from "@/lib/vocabulary";
import type { ApiError } from "@/lib/api";
import { useErrorText, useLabels, useLocale, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { common } from "@/lib/i18n/messages/common";

// ---------------------------------------------------------------- badges

/** The colour follows what the status means, so a custom status gets the tone of its kind. */
const KIND_TONE: Record<StatusKind, string> = {
  NEW: "tone-info",
  ASSIGNED: "tone-accent",
  IN_PROGRESS: "tone-warning",
  WAITING_REQUESTER: "tone-serious",
  ON_HOLD: "tone-serious",
  RESOLVED: "tone-success",
  CLOSED: "tone-neutral",
};

const PRIORITY_TONE: Record<string, string> = {
  LOW: "tone-neutral",
  MEDIUM: "tone-warning",
  HIGH: "tone-serious",
  CRITICAL: "tone-danger",
};

const SLA_TONE: Record<SlaState, string> = {
  ON_TRACK: "tone-success",
  APPROACHING: "tone-warning",
  BREACHED: "tone-danger",
  MET: "tone-neutral",
  PAUSED: "tone-info",
};

/** French typography puts a space before the colon. */
const PREFIX_SEPARATOR = { fr: " : ", en: ": " } as const;

const SLA_ICON: Record<SlaState, string> = { ON_TRACK: "●", APPROACHING: "◐", BREACHED: "▲", MET: "✓", PAUSED: "Ⅱ" };

export function StatusBadge({ status, kind }: { status: TicketStatus; kind?: StatusKind }) {
  const labels = useLabels();
  const vocabulary = useVocabulary();
  const k = kind ?? kindOf(status, vocabulary) ?? "IN_PROGRESS";
  return <span className={`badge badge-dot ${KIND_TONE[k]}`}>{labels.status[status]}</span>;
}

export function PriorityBadge({ priority }: { priority: TicketPriority }) {
  const labels = useLabels();
  return (
    <span className={`badge ${PRIORITY_TONE[priority] ?? "tone-accent"}`}>
      {priority === "CRITICAL" ? "⚠ " : ""}
      {labels.priority[priority]}
    </span>
  );
}

export function SlaBadge({ state, prefix }: { state: SlaState; prefix?: string }) {
  const labels = useLabels();
  const locale = useLocale();
  return (
    <span className={`badge ${SLA_TONE[state]}`}>
      <span aria-hidden="true">{SLA_ICON[state]}</span>
      {prefix ? `${prefix}${PREFIX_SEPARATOR[locale]}` : ""}
      {labels.sla[state]}
    </span>
  );
}

export function RoleBadge({ role }: { role: Role | null | undefined }) {
  const labels = useLabels();
  if (!role) return null;
  return <span className="badge tone-neutral">{labels.role[role]}</span>;
}

export function Avatar({ name, small }: { name: string; small?: boolean }) {
  const initials = name
    .split(/\s+/)
    .map((p) => p[0])
    .filter(Boolean)
    .slice(0, 2)
    .join("")
    .toUpperCase();
  return (
    <span className={`avatar${small ? " avatar-sm" : ""}`} aria-hidden="true">
      {initials}
    </span>
  );
}

// ---------------------------------------------------------------- states

export function Loading({ label, lines = 3 }: { label?: string; lines?: number }) {
  const t = useMessages(common);
  return (
    <div role="status" aria-live="polite" className="stack-sm">
      <span className="sr-only">{label ?? t.loading}</span>
      {Array.from({ length: lines }).map((_, i) => (
        <div key={i} className="skeleton" style={{ width: `${90 - i * 15}%` }} />
      ))}
    </div>
  );
}

export function ErrorState({ error, onRetry }: { error: ApiError | Error; onRetry?: () => void }) {
  const t = useMessages(common);
  const errorText = useErrorText();
  const forbidden = "status" in error && (error.status === 403 || error.status === 404);
  return (
    <div className="alert alert-danger" role="alert">
      <div className="grow">
        <strong>{forbidden ? t.accessImpossible : t.errorTitle}</strong>
        <span>{errorText(error)}</span>
      </div>
      {onRetry && !forbidden && (
        <button type="button" className="btn btn-secondary btn-sm" onClick={onRetry}>
          {t.retry}
        </button>
      )}
    </div>
  );
}

export function EmptyState({ icon = "○", title, children }: { icon?: string; title: string; children?: ReactNode }) {
  return (
    <div className="state">
      <div className="state-icon" aria-hidden="true">
        {icon}
      </div>
      <p>
        <strong>{title}</strong>
      </p>
      {children && <div className="small" style={{ marginTop: 6 }}>{children}</div>}
    </div>
  );
}

export function Alert({
  tone = "info",
  title,
  children,
  action,
}: {
  tone?: "info" | "success" | "warning" | "danger";
  title?: string;
  children?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className={`alert alert-${tone}`} role={tone === "danger" ? "alert" : "status"}>
      <div className="grow">
        {title && <strong>{title}</strong>}
        {children}
      </div>
      {action}
    </div>
  );
}

// ---------------------------------------------------------------- toasts (AC-UX-012: timely feedback)

interface Toast {
  id: number;
  text: string;
  error?: boolean;
}

const ToastContext = createContext<(text: string, error?: boolean) => void>(() => {});

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const counter = useRef(0);
  const push = useCallback((text: string, error = false) => {
    const id = ++counter.current;
    setToasts((t) => [...t, { id, text, error }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), error ? 7000 : 4000);
  }, []);
  return (
    <ToastContext.Provider value={push}>
      {children}
      <div className="toast-region" role="status" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast${t.error ? " toast-error" : ""}`}>
            {t.text}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export const useToast = () => useContext(ToastContext);

// ---------------------------------------------------------------- confirm dialog (AC-SB-021: deliberate final actions)

export function ConfirmDialog({
  open,
  title,
  children,
  confirmLabel,
  danger,
  busy,
  confirmDisabled,
  onConfirm,
  onCancel,
}: {
  open: boolean;
  title: string;
  children?: ReactNode;
  confirmLabel: string;
  danger?: boolean;
  busy?: boolean;
  confirmDisabled?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const t = useMessages(common);
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    if (open && !d.open) {
      if (typeof d.showModal === "function") d.showModal();
      else d.setAttribute("open", "");
    } else if (!open && d.open) {
      if (typeof d.close === "function") d.close();
      else d.removeAttribute("open");
    }
  }, [open]);
  return (
    <dialog ref={ref} className="dialog" onCancel={onCancel} aria-labelledby="confirm-title">
      <div className="stack">
        <h2 id="confirm-title">{title}</h2>
        {children}
        <div className="row" style={{ justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={busy}>
            {t.cancel}
          </button>
          <button
            type="button"
            className={`btn ${danger ? "btn-danger" : "btn-primary"}`}
            onClick={onConfirm}
            disabled={busy || confirmDisabled}
          >
            {busy ? t.pleaseWait : confirmLabel}
          </button>
        </div>
      </div>
    </dialog>
  );
}

// ---------------------------------------------------------------- pagination

export function Pagination({
  page,
  totalPages,
  totalItems,
  size,
  onPage,
}: {
  page: number;
  totalPages: number;
  totalItems: number;
  size: number;
  onPage: (p: number) => void;
}) {
  const t = useMessages(common);
  if (totalItems === 0) return null;
  const from = page * size + 1;
  const to = Math.min(totalItems, (page + 1) * size);
  return (
    <nav className="pagination" aria-label={t.pagination}>
      <span className="muted">{format(t.paginationRange, { from, to, total: totalItems })}</span>
      <div className="row">
        <button type="button" className="btn btn-secondary btn-sm" disabled={page === 0} onClick={() => onPage(page - 1)}>
          {t.previous}
        </button>
        <span className="small">{format(t.pageOf, { page: page + 1, pages: Math.max(totalPages, 1) })}</span>
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={page + 1 >= totalPages}
          onClick={() => onPage(page + 1)}
        >
          {t.next}
        </button>
      </div>
    </nav>
  );
}
