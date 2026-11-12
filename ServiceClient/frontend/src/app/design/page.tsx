"use client";

import { useEffect, useState, type ReactNode } from "react";
import { BarList, ColumnChart } from "@/components/Charts";
import { Preferences } from "@/components/Preferences";
import {
  Alert,
  Avatar,
  EmptyState,
  ErrorState,
  Loading,
  Pagination,
  PriorityBadge,
  RoleBadge,
  SlaBadge,
  StatusBadge,
  useToast,
} from "@/components/ui";
import { useLabels, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { design } from "@/lib/i18n/messages/design";
import type { Role, SlaState, TicketPriority, TicketStatus } from "@/lib/types";

/** Claude Design canvas with the mockups (EN/FR × light/dark × desktop/tablet/mobile) — docs/ux-ui/screens.md. */
const DESIGN_CANVAS_URL =
  process.env.NEXT_PUBLIC_DESIGN_CANVAS_URL ?? "https://claude.ai/artifact/GEnhzdFMLMLW8kWWd6hhcm";

const STATUSES: TicketStatus[] = ["NEW", "ASSIGNED", "IN_PROGRESS", "WAITING_FOR_CUSTOMER", "RESOLVED", "CLOSED"];
const PRIORITIES: TicketPriority[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
const SLA: SlaState[] = ["ON_TRACK", "APPROACHING", "BREACHED", "MET"];
const ROLES: Role[] = ["CUSTOMER", "SELLER", "DELIVERY_DRIVER", "SUPPORT_AGENT", "SUPPORT_MANAGER", "ADMINISTRATOR"];

const TOKEN_GROUPS = {
  brand: ["--brand", "--brand-strong", "--brand-soft", "--accent", "--accent-soft", "--chart-1", "--focus"],
  surfaces: ["--bg", "--surface", "--surface-2", "--border", "--border-strong", "--text", "--text-secondary", "--text-muted", "--sidebar", "--sidebar-text"],
  tones: ["--info", "--info-soft", "--success", "--success-soft", "--warning", "--warning-soft", "--serious", "--serious-soft", "--danger", "--danger-soft", "--neutral", "--neutral-soft", "--note", "--note-soft"],
} as const;

function Swatch({ token, value }: { token: string; value: string }) {
  return (
    <li className="swatch">
      <span className="swatch-chip" style={{ background: `var(${token})` }} aria-hidden="true" />
      <code className="xsmall">{token}</code>
      <span className="xsmall muted mono">{value || "…"}</span>
    </li>
  );
}

function Section({ id, title, children, hint }: { id: string; title: string; hint?: string; children: ReactNode }) {
  return (
    <section id={id} className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>{title}</h2>
      {hint && <p className="small muted">{hint}</p>}
      {children}
    </section>
  );
}

/**
 * Design catalogue: every token and shared component rendered with the real CSS and components,
 * in the current language and theme. It holds no data and needs no sign-in.
 */
export default function DesignCatalogue() {
  const t = useMessages(design);
  const labels = useLabels();
  const toast = useToast();
  const [values, setValues] = useState<Record<string, string>>({});
  const [priority, setPriority] = useState<TicketPriority>("MEDIUM");
  const [tab, setTab] = useState(0);
  const [page, setPage] = useState(0);

  // read the computed token values after paint and whenever the theme attribute changes
  useEffect(() => {
    const read = () => {
      const style = getComputedStyle(document.documentElement);
      const next: Record<string, string> = {};
      Object.values(TOKEN_GROUPS).flat().forEach((tk) => (next[tk] = style.getPropertyValue(tk).trim()));
      setValues(next);
    };
    read();
    const observer = new MutationObserver(read);
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ["data-theme"] });
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    media.addEventListener("change", read);
    return () => {
      observer.disconnect();
      media.removeEventListener("change", read);
    };
  }, []);

  const sections: [string, string][] = [
    ["colors", t.colors],
    ["typography", t.typography],
    ["spacing", t.spacing],
    ["buttons", t.buttons],
    ["badges", t.badges],
    ["forms", t.forms],
    ["feedback", t.feedback],
    ["data", t.data],
    ["conversation", t.conversation],
    ["navigation", t.navigation],
  ];

  return (
    <main className="design-page">
      <header className="row-between design-header">
        <div className="stack-sm">
          <span className="logo">
            <span className="logo-mark" aria-hidden="true">M</span> Maplenou
          </span>
          <h1>{t.title}</h1>
          <p className="secondary small" style={{ maxWidth: 640 }}>{t.intro}</p>
          {DESIGN_CANVAS_URL && (
            <a className="small" href={DESIGN_CANVAS_URL} target="_blank" rel="noreferrer">
              {t.designCanvas} ↗
            </a>
          )}
        </div>
        <Preferences variant="inline" />
      </header>

      <nav aria-label={t.toc} className="design-toc">
        <ul>
          {sections.map(([id, title]) => (
            <li key={id}>
              <a href={`#${id}`}>{title}</a>
            </li>
          ))}
        </ul>
      </nav>

      <div className="stack">
        <Section id="colors" title={t.colors} hint={t.colorsHint}>
          {(
            [
              ["brand", t.brand],
              ["surfaces", t.surfaces],
              ["tones", t.tones],
            ] as const
          ).map(([group, title]) => (
            <div key={group} className="stack-sm">
              <h3>{title}</h3>
              <ul className="swatches">
                {TOKEN_GROUPS[group].map((tk) => (
                  <Swatch key={tk} token={tk} value={values[tk]} />
                ))}
              </ul>
            </div>
          ))}
        </Section>

        <Section id="typography" title={t.typography}>
          <div className="stack-sm">
            <h1>{t.typeH1}</h1>
            <h2>{t.typeH2}</h2>
            <h3>{t.typeH3}</h3>
            <p>{t.typeBody}</p>
            <p className="small secondary">{t.typeSmall}</p>
            <p className="xsmall muted">{t.typeSmall}</p>
            <p className="eyebrow">{t.typeMono}</p>
            <p>
              <span className="ticket-number">TKT-2026-00042</span>
            </p>
          </div>
        </Section>

        <Section id="spacing" title={t.spacing}>
          <div className="row" style={{ alignItems: "flex-end", flexWrap: "wrap" }}>
            {[1, 2, 3, 4, 5, 6].map((n) => (
              <div key={n} className="stack-sm" style={{ alignItems: "center" }}>
                <span className="space-bar" style={{ width: `var(--space-${n})`, height: `var(--space-${n})` }} />
                <code className="xsmall">--space-{n}</code>
              </div>
            ))}
            {["--radius-sm", "--radius", "--radius-lg"].map((r) => (
              <div key={r} className="stack-sm" style={{ alignItems: "center" }}>
                <span className="radius-box" style={{ borderRadius: `var(${r})` }} />
                <code className="xsmall">{r}</code>
              </div>
            ))}
          </div>
        </Section>

        <Section id="buttons" title={t.buttons}>
          <div className="row" style={{ flexWrap: "wrap" }}>
            <button type="button" className="btn btn-primary">{t.primary}</button>
            <button type="button" className="btn btn-secondary">{t.secondary}</button>
            <button type="button" className="btn btn-tertiary">{t.tertiary}</button>
            <button type="button" className="btn btn-accent">{t.accent}</button>
            <button type="button" className="btn btn-danger">{t.danger}</button>
            <button type="button" className="btn btn-primary btn-sm">{t.small}</button>
            <button type="button" className="btn btn-primary" disabled>{t.disabled}</button>
          </div>
        </Section>

        <Section id="badges" title={t.badges} hint={t.badgesHint}>
          <h3>{t.statuses}</h3>
          <div className="row" style={{ flexWrap: "wrap" }}>
            {STATUSES.map((s) => <StatusBadge key={s} status={s} />)}
          </div>
          <h3>{t.priorities}</h3>
          <div className="row" style={{ flexWrap: "wrap" }}>
            {PRIORITIES.map((p) => <PriorityBadge key={p} priority={p} />)}
          </div>
          <h3>{t.sla}</h3>
          <div className="row" style={{ flexWrap: "wrap" }}>
            {SLA.map((s) => <SlaBadge key={s} state={s} />)}
          </div>
          <h3>{t.roles}</h3>
          <div className="row" style={{ flexWrap: "wrap" }}>
            {ROLES.map((r) => <RoleBadge key={r} role={r} />)}
            <Avatar name="Sophie N." />
            <Avatar name="Jean K." small />
            <span className="count-pill">3</span>
          </div>
        </Section>

        <Section id="forms" title={t.forms}>
          <div className="grid-2">
            <div className="stack">
              <div className="field">
                <label htmlFor="d-subject" className="required">{t.fieldLabel}</label>
                <input id="d-subject" className="input" placeholder={t.fieldPlaceholder} />
              </div>
              <div className="field">
                <label htmlFor="d-subject-err" className="required">{t.fieldLabel}</label>
                <input id="d-subject-err" className="input" aria-invalid="true" aria-describedby="d-err" />
                <span id="d-err" className="error">{t.fieldError}</span>
              </div>
              <div className="field">
                <label htmlFor="d-category">{t.selectLabel}</label>
                <select id="d-category" className="select" defaultValue="">
                  <option value="">{t.selectOption}</option>
                  <option>{t.catDelivery}</option>
                  <option>{t.catPayment}</option>
                </select>
              </div>
            </div>
            <div className="stack">
              <div className="field">
                <label htmlFor="d-desc">{t.textareaLabel}</label>
                <textarea id="d-desc" className="textarea" />
              </div>
              <fieldset className="field" style={{ border: 0, padding: 0, margin: 0 }}>
                <legend className="label" style={{ marginBottom: 6 }}>{t.segmentedLabel}</legend>
                <div className="segmented">
                  {(["LOW", "MEDIUM", "HIGH"] as TicketPriority[]).map((p) => (
                    <label key={p}>
                      <input type="radio" name="d-priority" checked={priority === p} onChange={() => setPriority(p)} />
                      {labels.priority[p]}
                    </label>
                  ))}
                </div>
              </fieldset>
              <label className="checkbox">
                <input type="checkbox" />
                <span>{t.checkboxLabel}</span>
              </label>
            </div>
          </div>
        </Section>

        <Section id="feedback" title={t.feedback}>
          <Alert tone="info">{t.alertInfo}</Alert>
          <Alert tone="success">{t.alertSuccess}</Alert>
          <Alert tone="warning">{t.alertWarning}</Alert>
          <Alert tone="danger">{t.alertDanger}</Alert>
          <div>
            <button type="button" className="btn btn-secondary btn-sm" onClick={() => toast(t.toastText)}>
              {t.showToast}
            </button>
          </div>
          <div className="grid-3">
            <div className="card card-tight"><Loading /></div>
            <div className="card card-tight">
              <EmptyState icon="📭" title={t.emptyTitle}>{t.emptyText}</EmptyState>
            </div>
            <div className="card card-tight">
              <ErrorState error={new Error(t.sampleError)} onRetry={() => toast(t.toastText)} />
            </div>
          </div>
        </Section>

        <Section id="data" title={t.data}>
          <div className="kpis">
            <div className="kpi">
              <div className="eyebrow">{t.kpiOpen}</div>
              <div className="kpi-value">12</div>
              <div className="kpi-note muted">{format(t.kpiNew, { count: 3 })}</div>
            </div>
            <div className="kpi">
              <div className="eyebrow">{t.kpiBreached}</div>
              <div className="kpi-value" style={{ color: "var(--danger)" }}>2</div>
            </div>
          </div>
          <div className="grid-2">
            <BarList
              label={t.chartLabel}
              items={[
                { key: "d", label: t.catDelivery, value: 6 },
                { key: "p", label: t.catPayment, value: 4 },
                { key: "o", label: t.catOrder, value: 2 },
              ]}
            />
            <ColumnChart
              label={t.chartDays}
              points={["01/10", "02/10", "03/10", "04/10", "05/10", "06/10", "07/10"].map((d, i) => ({
                key: d,
                label: d,
                value: [3, 5, 2, 6, 4, 1, 3][i],
              }))}
            />
          </div>
          <dl className="kv">
            <dt>{t.kvName}</dt>
            <dd>Jean K.</dd>
            <dt>{t.kvRole}</dt>
            <dd>{labels.role.CUSTOMER}</dd>
          </dl>
          <Pagination page={page} totalPages={3} totalItems={45} size={20} onPage={setPage} />
        </Section>

        <Section id="conversation" title={t.conversation}>
          <ul className="thread" style={{ listStyle: "none", margin: 0, padding: 0 }}>
            <li className="msg mine">
              <div className="msg-meta"><strong>Jean K.</strong><span className="badge tone-neutral">{labels.role.CUSTOMER}</span></div>
              <div className="msg-bubble">{t.msgCustomer}</div>
            </li>
            <li className="msg support-msg">
              <div className="msg-meta"><strong>Sophie N.</strong><span className="badge tone-neutral">{labels.role.SUPPORT_AGENT}</span></div>
              <div className="msg-bubble">{t.msgAgent}</div>
            </li>
            <li className="msg internal">
              <div className="msg-meta"><span className="internal-label">{t.internalNote}</span><strong>Sophie N.</strong></div>
              <div className="msg-bubble">{t.msgNote}</div>
            </li>
          </ul>
        </Section>

        <Section id="navigation" title={t.navigation}>
          <div className="tablist" role="tablist" aria-label={t.navigation}>
            {[t.tabAll, t.tabOpen, t.tabClosed].map((label, i) => (
              <button key={label} type="button" role="tab" aria-selected={tab === i} onClick={() => setTab(i)}>
                {label}
              </button>
            ))}
          </div>
        </Section>
      </div>
    </main>
  );
}
