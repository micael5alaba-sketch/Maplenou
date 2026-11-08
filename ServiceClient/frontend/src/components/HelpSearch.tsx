"use client";

import { useState } from "react";
import { useDocsUrl, useLocale, useMessages } from "@/lib/i18n/client";
import { helpSearch } from "@/lib/i18n/messages/helpSearch";
import type { HelpArticle } from "@/lib/types";

const ARTICLES = ["create-ticket/", "follow-tickets/", "reopen-and-close/", "notifications/", "faq/"] as const;

/**
 * Help-centre search (public user guide, no language model): GET {DOCS_BASE_URL}/api/assistant/search.
 * Reads public documentation only — nothing about the user leaves the app. Without the docs service, only the
 * article links are shown.
 */
export function HelpSearch() {
  const t = useMessages(helpSearch);
  const locale = useLocale();
  const docsUrl = useDocsUrl();
  const [q, setQ] = useState("");
  const [results, setResults] = useState<HelpArticle[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [busy, setBusy] = useState(false);
  const base = docsUrl("user").replace(/\/user\/(fr|en)\/$/, "");

  async function search(e: React.FormEvent) {
    e.preventDefault();
    const query = q.trim();
    if (!query) return;
    setBusy(true);
    setFailed(false);
    try {
      const url = `${base}/api/assistant/search?${new URLSearchParams({ q: query, site: "user", lang: locale, limit: "6" })}`;
      const res = await fetch(url, { cache: "no-store" });
      if (!res.ok) throw new Error(String(res.status));
      setResults(((await res.json()) as { items: HelpArticle[] }).items);
    } catch {
      setFailed(true);
      setResults(null);
    } finally {
      setBusy(false);
    }
  }

  const titles: Record<(typeof ARTICLES)[number], string> = {
    "create-ticket/": t.articleCreate,
    "follow-tickets/": t.articleFollow,
    "reopen-and-close/": t.articleReopen,
    "notifications/": t.articleNotifications,
    "faq/": t.articleFaq,
  };

  return (
    <section className="card stack-sm" aria-labelledby="help-search-title">
      <h2 id="help-search-title" style={{ fontSize: "1rem" }}>{t.title}</h2>
      <form role="search" className="row" onSubmit={search}>
        <label className="sr-only" htmlFor="help-q">{t.label}</label>
        <input id="help-q" className="input grow" type="search" value={q} maxLength={500} placeholder={t.placeholder}
               onChange={(e) => setQ(e.target.value)} />
        <button type="submit" className="btn btn-secondary btn-sm" disabled={busy || !q.trim()}>{t.search}</button>
      </form>
      <div aria-live="polite">
        {failed && <p className="small muted">{t.unavailable}</p>}
        {results && results.length === 0 && <p className="small muted">{t.noResult}</p>}
        {results && results.length > 0 && (
          <ul className="list-links">
            {results.map((r) => (
              <li key={r.url}>
                <a className="list-link" href={r.url} target="_blank" rel="noreferrer">
                  <span className="grow">
                    <strong>{r.title}</strong>
                    <span className="sr-only"> ({t.newTab})</span>
                    <br />
                    <span className="xsmall muted">{r.breadcrumb}</span>
                    <br />
                    <span className="small secondary">{r.excerpt}</span>
                  </span>
                  <span className="chev" aria-hidden="true">↗</span>
                </a>
              </li>
            ))}
          </ul>
        )}
      </div>
      <details className="small">
        <summary>{t.popular}</summary>
        <ul style={{ paddingLeft: 16 }}>
          {ARTICLES.map((a) => (
            <li key={a}><a href={docsUrl("user", a)} target="_blank" rel="noreferrer">{titles[a]}</a></li>
          ))}
        </ul>
      </details>
    </section>
  );
}
