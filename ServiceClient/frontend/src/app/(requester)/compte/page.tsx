"use client";

import { useApi } from "@/lib/useApi";
import { useDocsUrl, useLabels, useMessages } from "@/lib/i18n/client";
import { common } from "@/lib/i18n/messages/common";
import { account } from "@/lib/i18n/messages/requester";
import type { User } from "@/lib/types";
import { ErrorState, Loading } from "@/components/ui";
import { logout } from "@/components/session";
import { Preferences } from "@/components/Preferences";

/** C2 — Profile / account (identity comes from the Maplenou account; read-only here). */
export default function AccountPage() {
  const t = useMessages(account);
  const c = useMessages(common);
  const labels = useLabels();
  const docsUrl = useDocsUrl();
  const me = useApi<User>("/me");
  return (
    <div className="stack">
      <h1>{t.title}</h1>
      {me.loading && <Loading />}
      {me.error && <ErrorState error={me.error} onRetry={me.reload} />}
      {me.data && (
        <div className="card">
          <dl className="kv">
            <dt>{t.name}</dt><dd>{me.data.displayName}</dd>
            <dt>{t.profile}</dt><dd>{labels.role[me.data.role]}</dd>
            <dt>{t.email}</dt><dd>{me.data.email ?? c.none}</dd>
            <dt>{t.phone}</dt><dd>{me.data.phone ?? c.none}</dd>
          </dl>
          <p className="xsmall muted" style={{ marginTop: 12 }}>{t.source}</p>
        </div>
      )}
      <section className="card stack-sm" aria-labelledby="prefs-title">
        <h2 id="prefs-title">{c.preferences}</h2>
        <Preferences variant="stacked" />
        <a className="small" href={docsUrl("user")} target="_blank" rel="noreferrer">
          {c.helpCenter} ↗<span className="sr-only"> {c.opensInNewTab}</span>
        </a>
      </section>
      <button type="button" className="btn btn-secondary" onClick={logout}>{t.logout}</button>
    </div>
  );
}
