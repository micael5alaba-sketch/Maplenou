"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, type ReactNode } from "react";
import { isSupervisor } from "@/lib/labels";
import { useDocsUrl, useLabels, useMessages } from "@/lib/i18n/client";
import { common } from "@/lib/i18n/messages/common";
import { shell } from "@/lib/i18n/messages/shell";
import { Preferences } from "./Preferences";
import { VocabularyProvider } from "@/lib/vocabulary";
import { useApi } from "@/lib/useApi";
import type { Dashboard } from "@/lib/types";
import { logout, useSession } from "./session";
import { Avatar } from "./ui";

/** Support back-office layout (mockups 12–17). Menu entries follow the caller's role; the API enforces it. */
/** The administrators' vocabulary (statuses, priorities) is loaded once for every screen of the shell. */
export function SupportShell({ children }: { children: ReactNode }) {
  return (
    <VocabularyProvider>
      <SupportShellLayout>{children}</SupportShellLayout>
    </VocabularyProvider>
  );
}

function SupportShellLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const session = useSession();
  const [open, setOpen] = useState(false);
  const c = useMessages(common);
  const t = useMessages(shell);
  const labels = useLabels();
  const docsUrl = useDocsUrl();
  const counts = useApi<Dashboard>(`/dashboard?p=${pathname}`);
  const unread = useApi<{ unread: number }>(`/notifications/unread-count?p=${pathname}`);
  const supervisor = isSupervisor(session.role);

  const item = (href: string, label: string, count?: number, exact = false) => {
    const active = exact ? pathname === href : pathname === href || pathname.startsWith(`${href}/`);
    return (
      <li>
        <Link href={href} aria-current={active ? "page" : undefined} onClick={() => setOpen(false)}>
          {label}
          {!!count && <span className="count-pill">{count}</span>}
        </Link>
      </li>
    );
  };

  return (
    <div className="support">
      <a className="skip-link" href="#main">
        {c.skipToContent}
      </a>
      <aside className={`sidebar${open ? " open" : ""}`}>
        <div className="sidebar-top">
          <Link href="/support" className="logo">
            <span className="logo-mark" aria-hidden="true">M</span> {c.appName}
          </Link>
          <button
            type="button"
            className="btn btn-secondary btn-sm mobile-only"
            aria-expanded={open}
            aria-controls="support-nav"
            onClick={() => setOpen(!open)}
          >
            {c.menu}
          </button>
        </div>
        <nav id="support-nav" aria-label={t.supportNav}>
          <div className="sidebar-section">{t.sectionSupport}</div>
          <ul>
            {item("/support", t.dashboard, undefined, true)}
            {item("/support/tickets", t.queue, counts.data?.openCount)}
            {item("/support/mes-tickets", t.navMyTickets, counts.data?.myOpenCount)}
            {item("/support/notifications", t.navNotifications, unread.data?.unread)}
          </ul>
          {supervisor && (
            <>
              <div className="sidebar-section">{t.sectionAdmin}</div>
              <ul>
                {item("/support/rapports", t.reports)}
                {item("/support/admin/agents", t.agents)}
                {item("/support/admin/categories", t.categories)}
                {item("/support/admin/calendrier", t.slaCalendar)}
                {item("/support/admin/flux", t.workflow)}
                {item("/support/admin/modeles", t.templates)}
                {session.role === "ADMINISTRATOR" && item("/support/admin/permissions", t.permissions)}
              </ul>
            </>
          )}
          <ul>
            <li>
              <a href={docsUrl("staff")} target="_blank" rel="noreferrer">
                {c.staffGuide} ↗<span className="sr-only"> {c.opensInNewTab}</span>
              </a>
            </li>
          </ul>
        </nav>
        <Preferences variant="sidebar" />
        <div className="sidebar-user">
          <Avatar name={session.name} />
          <div className="grow">
            <div style={{ color: "#fff", fontWeight: 600 }}>{session.name}</div>
            <div className="xsmall">{labels.role[session.role]}</div>
          </div>
          <button type="button" className="btn btn-tertiary btn-sm" style={{ color: "var(--sidebar-text)" }} onClick={logout}>
            {c.logoutShort}
          </button>
        </div>
      </aside>
      <div className="support-main">{children}</div>
    </div>
  );
}

export function TopBar({ title, children }: { title: ReactNode; children?: ReactNode }) {
  return (
    <header className="topbar">
      <h1 className="grow">{title}</h1>
      {children}
    </header>
  );
}
