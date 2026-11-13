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
import { BrandLogo } from "./BrandLogo";
import { NavIcon, type NavIconName } from "./NavIcon";

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

  const item = (href: string, label: string, icon: NavIconName, count?: number, exact = false) => {
    const active = exact ? pathname === href : pathname === href || pathname.startsWith(`${href}/`);
    return (
      <li>
        <Link href={href} aria-current={active ? "page" : undefined} onClick={() => setOpen(false)}>
          <NavIcon name={icon} />
          <span className="nav-label">{label}</span>
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
          <Link href="/support" className="brand" aria-label={`${c.appName} — ${c.serviceName}`}>
            <BrandLogo variant="white" height={34} alt={c.appName} />
            <span className="brand-service">{c.serviceName}</span>
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
            {item("/support", t.dashboard, "dashboard", undefined, true)}
            {item("/support/tickets", t.queue, "queue", counts.data?.openCount)}
            {item("/support/mes-tickets", t.navMyTickets, "mine", counts.data?.myOpenCount)}
            {item("/support/notifications", t.navNotifications, "bell", unread.data?.unread)}
          </ul>
          {supervisor && (
            <>
              <div className="sidebar-section">{t.sectionAdmin}</div>
              <ul>
                {item("/support/rapports", t.reports, "reports")}
                {item("/support/admin/agents", t.agents, "agents")}
                {item("/support/admin/categories", t.categories, "categories")}
                {item("/support/admin/calendrier", t.slaCalendar, "calendar")}
                {item("/support/admin/flux", t.workflow, "workflow")}
                {item("/support/admin/modeles", t.templates, "templates")}
                {session.role === "ADMINISTRATOR" && item("/support/admin/permissions", t.permissions, "shield")}
              </ul>
            </>
          )}
          <ul>
            <li>
              <a href={docsUrl("staff")} target="_blank" rel="noreferrer">
                <NavIcon name="book" />
                <span className="nav-label">{c.staffGuide} ↗</span>
                <span className="sr-only"> {c.opensInNewTab}</span>
              </a>
            </li>
          </ul>
        </nav>
        <Preferences variant="sidebar" />
        <div className="sidebar-user">
          <Avatar name={session.name} />
          <div className="grow">
            <div className="sidebar-user-name">{session.name}</div>
            <div className="xsmall sidebar-user-role">{labels.role[session.role]}</div>
          </div>
          <button type="button" className="sidebar-logout" onClick={logout} title={c.logoutShort} aria-label={c.logoutShort}>
            <NavIcon name="logout" size={17} />
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
