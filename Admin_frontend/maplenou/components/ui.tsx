"use client";

import type { ReactNode } from "react";

export function PageHeader({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="mb-6 flex items-start justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold text-foreground">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-muted">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-2xl bg-surface p-5 shadow-sm ring-1 ring-border ${className}`}>{children}</div>
  );
}

const BADGE_STYLES: Record<string, string> = {
  PENDING: "bg-accent/10 text-accent",
  APPROVED: "bg-primary/10 text-primary",
  VALID: "bg-primary/10 text-primary",
  PAID: "bg-primary/10 text-primary",
  RESOLVED: "bg-primary/10 text-primary",
  ACTIVE: "bg-primary/10 text-primary",
  REJECTED: "bg-error/10 text-error",
  FAILED: "bg-error/10 text-error",
  SUSPENDED: "bg-error/10 text-error",
  PROCESSING: "bg-accent/10 text-accent",
  IN_PROGRESS: "bg-accent/10 text-accent",
  OPEN: "bg-accent/10 text-accent",
  PREPARING: "bg-accent/10 text-accent",
  READY_FOR_PICKUP: "bg-accent/10 text-accent",
  IN_DELIVERY: "bg-accent/10 text-accent",
  DELIVERED: "bg-primary/10 text-primary",
  CANCELLED: "bg-error/10 text-error",
  RETURN_REQUESTED: "bg-error/10 text-error",
  RETURNED: "bg-error/10 text-error",
  default: "bg-muted/10 text-muted",
};

export function StatusBadge({ status, label }: { status: string | null | undefined; label?: string }) {
  if (!status) return <span className="text-sm text-muted">—</span>;
  const style = BADGE_STYLES[status] ?? BADGE_STYLES.default;
  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${style}`}>
      {label ?? status}
    </span>
  );
}

export function Button({
  children,
  onClick,
  variant = "primary",
  disabled,
  type = "button",
}: {
  children: ReactNode;
  onClick?: () => void;
  variant?: "primary" | "secondary" | "danger";
  disabled?: boolean;
  type?: "button" | "submit";
}) {
  const styles: Record<string, string> = {
    primary: "bg-primary text-white hover:bg-primary-dark",
    secondary: "border border-border text-foreground hover:bg-input-fill",
    danger: "bg-error text-white hover:opacity-90",
  };
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`rounded-full px-4 py-2 text-sm font-semibold transition disabled:opacity-50 ${styles[variant]}`}
    >
      {children}
    </button>
  );
}

export function LoadingState() {
  return <div className="flex justify-center py-16 text-sm text-muted">Chargement...</div>;
}

export function ErrorState({ message }: { message: string }) {
  return (
    <div className="rounded-xl border border-error/30 bg-error/10 px-4 py-3 text-sm text-error">
      {message}
    </div>
  );
}

export function EmptyState({ message }: { message: string }) {
  return <div className="py-16 text-center text-sm text-muted">{message}</div>;
}

export function Table({ headers, children }: { headers: string[]; children: ReactNode }) {
  return (
    <div className="overflow-x-auto rounded-xl ring-1 ring-border">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-border bg-input-fill">
            {headers.map((h) => (
              <th key={h} className="px-4 py-3 font-semibold text-foreground">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-border">{children}</tbody>
      </table>
    </div>
  );
}

export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;

  const pages = new Set<number>([0, totalPages - 1, page, page - 1, page + 1]);
  const items = [...pages].filter((p) => p >= 0 && p < totalPages).sort((a, b) => a - b);

  return (
    <div className="mt-4 flex items-center justify-center gap-1.5">
      <button
        onClick={() => onChange(page - 1)}
        disabled={page === 0}
        className="rounded-lg px-3 py-1.5 text-sm font-medium text-foreground transition hover:bg-input-fill disabled:opacity-40"
      >
        Précédent
      </button>
      {items.map((p, i) => (
        <span key={p} className="flex items-center gap-1.5">
          {i > 0 && items[i - 1] !== p - 1 && <span className="px-1 text-muted">…</span>}
          <button
            onClick={() => onChange(p)}
            className={`h-8 w-8 rounded-lg text-sm font-semibold transition ${
              p === page ? "bg-primary text-white" : "text-foreground hover:bg-input-fill"
            }`}
          >
            {p + 1}
          </button>
        </span>
      ))}
      <button
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
        className="rounded-lg px-3 py-1.5 text-sm font-medium text-foreground transition hover:bg-input-fill disabled:opacity-40"
      >
        Suivant
      </button>
    </div>
  );
}

export function formatFcfa(amount: number | null | undefined): string {
  if (amount === null || amount === undefined) return "—";
  return `${Math.round(amount).toLocaleString("fr-FR")} FCFA`;
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" });
}
