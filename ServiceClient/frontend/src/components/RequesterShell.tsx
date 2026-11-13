"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { useApi } from "@/lib/useApi";
import { useDocsUrl, useLabels, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { common } from "@/lib/i18n/messages/common";
import { shell } from "@/lib/i18n/messages/shell";
import { Preferences } from "./Preferences";
import { VocabularyProvider } from "@/lib/vocabulary";
import { logout, useSession } from "./session";
import { BrandLogo } from "./BrandLogo";

/** Requester layout: mobile-first, bottom navigation (CS-02 information architecture). */
/** The administrators' vocabulary (statuses, priorities) is loaded once for every screen of the shell. */
export function RequesterShell({ children }: { children: ReactNode }) {
  return (
    <VocabularyProvider>
      <RequesterShellLayout>{children}</RequesterShellLayout>
    </VocabularyProvider>
  );
}

function RequesterShellLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const session = useSession();
  const c = useMessages(common);
  const t = useMessages(shell);
  const labels = useLabels();
  const docsUrl = useDocsUrl();
  const unread = useApi<{ unread: number }>(`/notifications/unread-count?p=${pathname}`);
  const nav = [
    { href: "/aide", label: t.navHelp, icon: "?", match: (p: string) => p === "/aide" || p === "/aide/nouveau" },
    { href: "/aide/tickets", label: t.navMyTickets, icon: "☰", match: (p: string) => p.startsWith("/aide/tickets") },
    { href: "/notifications", label: t.navNotifications, icon: "🔔", match: (p: string) => p === "/notifications" },
    { href: "/compte", label: t.navAccount, icon: "◉", match: (p: string) => p === "/compte" },
  ];
  return (
    <div className="req-shell">
      <a className="skip-link" href="#main">
        {c.skipToContent}
      </a>
      <header className="req-header">
        <div className="req-header-inner">
          <Link href="/aide" className="brand-inline" aria-label={`${c.appName} — ${c.serviceName}`}>
            <BrandLogo height={30} alt={c.appName} />
          </Link>
          <span className="muted small grow">
            <span className="hide-narrow">{c.serviceName}</span>
          </span>
          <a
            className="btn btn-tertiary btn-sm docs-link hide-narrow"
            href={docsUrl("user")}
            target="_blank"
            rel="noreferrer"
          >
            {c.helpCenter}
            <span className="sr-only"> {c.opensInNewTab}</span>
          </a>
          <span className="hide-narrow">
            <Preferences />
          </span>
          <span className="small secondary hide-narrow" title={labels.role[session.role]}>
            {session.name}
          </span>
          <button type="button" className="btn btn-tertiary btn-sm" onClick={logout}>
            {c.logout}
          </button>
        </div>
      </header>
      <main id="main" className="req-main">
        {children}
      </main>
      <nav className="req-nav" aria-label={t.mainNav}>
        <ul>
          {nav.map((n) => (
            <li key={n.href}>
              <Link href={n.href} aria-current={n.match(pathname) ? "page" : undefined}>
                <span className="nav-icon" aria-hidden="true">
                  {n.icon}
                </span>
                {n.label}
                {n.href === "/notifications" && !!unread.data?.unread && (
                  <span className="count-pill" aria-label={format(c.unreadCount, { count: unread.data.unread })}>
                    {unread.data.unread}
                  </span>
                )}
              </Link>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  );
}
