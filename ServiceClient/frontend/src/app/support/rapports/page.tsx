"use client";

import { useState } from "react";
import { TopBar } from "@/components/SupportShell";
import { BarList, ColumnChart } from "@/components/Charts";
import { ErrorState, Loading } from "@/components/ui";
import { useFormat, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { common } from "@/lib/i18n/messages/common";
import { reports } from "@/lib/i18n/messages/reports";
import { useApi } from "@/lib/useApi";
import type { Report } from "@/lib/types";

function isoDay(d: Date) {
  return d.toISOString().slice(0, 10);
}

/** Basic support report (managers/administrators). Not advanced analytics — see CS-01 exclusions. */
export default function ReportsPage() {
  const t = useMessages(reports);
  const c = useMessages(common);
  const f = useFormat();
  const today = new Date();
  const [from, setFrom] = useState(isoDay(new Date(today.getTime() - 29 * 86400000)));
  const [to, setTo] = useState(isoDay(today));
  const report = useApi<Report>(`/reports/summary?from=${from}&to=${to}`);
  // length of an SLA day: 24 h with the 24/7 clock (default), 10 h with business hours
  const sla = useApi<{ mode: "always" | "business-hours"; minutesPerDay: number }>("/admin/sla-settings");
  const perDay = sla.data?.minutesPerDay ?? 1440;
  const r = report.data;
  const pct = (v: number | null) => (v == null ? c.none : format(t.percent, { value: f.number(v) }));

  return (
    <>
      <TopBar title={t.title}>
        <div className="row small">
          <label htmlFor="from">{t.from}</label>
          <input id="from" type="date" className="input" style={{ width: "auto" }} value={from} max={to} onChange={(e) => setFrom(e.target.value)} />
          <label htmlFor="to">{t.to}</label>
          <input id="to" type="date" className="input" style={{ width: "auto" }} value={to} min={from} onChange={(e) => setTo(e.target.value)} />
        </div>
      </TopBar>
      <div className="support-content">
        {report.loading && !r && <Loading lines={5} />}
        {report.error && <ErrorState error={report.error} onRetry={report.reload} />}
        {r && (
          <>
            <section className="kpis" aria-label={t.kpis}>
              <div className="kpi"><div className="eyebrow">{t.created}</div><div className="kpi-value">{r.created}</div></div>
              <div className="kpi"><div className="eyebrow">{t.resolved}</div><div className="kpi-value">{r.resolved}</div>
                <div className="kpi-note muted">{format(t.stillOpen, { count: r.stillOpen })}</div></div>
              <div className="kpi"><div className="eyebrow">{t.avgFirstResponse}</div><div className="kpi-value">{f.businessMinutes(r.avgFirstResponseBusinessMinutes, perDay)}</div>
                <div className="kpi-note muted">{format(t.slaMet, { value: pct(r.firstResponseSlaMetPercent) })}</div></div>
              <div className="kpi"><div className="eyebrow">{t.avgResolution}</div><div className="kpi-value">{f.businessMinutes(r.avgResolutionBusinessMinutes, perDay)}</div>
                <div className="kpi-note muted">{format(t.slaMet, { value: pct(r.resolutionSlaMetPercent) })}</div></div>
              <div className="kpi"><div className="eyebrow">{t.satisfaction}</div>
                <div className="kpi-value">{r.satisfaction?.average != null ? `${f.number(Math.round(r.satisfaction.average * 10) / 10)} / 5` : "—"}</div>
                <div className="kpi-note muted">{r.satisfaction?.count ? format(t.satisfactionNote, { count: r.satisfaction.count, value: pct(r.satisfaction.satisfiedPercent) }) : t.satisfactionNone}</div></div>
            </section>
            <div className="grid-2">
              <section className="card" aria-labelledby="per-day">
                <h2 id="per-day">{t.createdPerDay}</h2>
                <ColumnChart label={t.createdPerDay}
                             points={Object.entries(r.createdPerDay).map(([day, v]) => ({ key: day, label: day.slice(8, 10) + "/" + day.slice(5, 7), value: v }))} />
              </section>
              <section className="card" aria-labelledby="per-cat">
                <h2 id="per-cat">{t.byCategory}</h2>
                {r.byCategory.length === 0 ? <p className="muted small">{t.noTickets}</p> : (
                  <BarList label={t.byCategory} items={r.byCategory.map((cat) => ({ key: cat.code, label: cat.name, value: cat.count }))} />
                )}
              </section>
            </div>
            <p className="xsmall muted">{sla.data?.mode === "business-hours" ? t.footnote : t.footnoteAlways}</p>
          </>
        )}
      </div>
    </>
  );
}
