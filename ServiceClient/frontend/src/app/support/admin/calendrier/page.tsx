"use client";

import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { useErrorText, useFormat, useLabels, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { adminCalendar } from "@/lib/i18n/messages/admin";
import { useApi } from "@/lib/useApi";
import type { Holiday, SlaPolicy } from "@/lib/types";
import { TopBar } from "@/components/SupportShell";
import { useSession } from "@/components/session";
import { EmptyState, ErrorState, Loading, useToast } from "@/components/ui";

/** Business calendar for SLA (DEC-001): officially recognized public holidays. */
export default function CalendarPage() {
  const t = useMessages(adminCalendar);
  const f = useFormat();
  const errorText = useErrorText();
  const session = useSession();
  const toast = useToast();
  const holidays = useApi<Holiday[]>("/admin/holidays");
  const settings = useApi<{ mode: "always" | "business-hours" }>("/admin/sla-settings");
  const [date, setDate] = useState("");
  const [label, setLabel] = useState("");
  const admin = session.role === "ADMINISTRATOR";

  async function add(e: React.FormEvent) {
    e.preventDefault();
    try {
      await api.put(`/admin/holidays/${date}`, { label });
      toast(t.saved);
      setDate("");
      setLabel("");
      await holidays.reload();
    } catch (err) {
      toast(err instanceof ApiError ? errorText(err) : t.saveFailed, true);
    }
  }

  async function remove(h: Holiday) {
    try {
      await api.del(`/admin/holidays/${h.date}`);
      toast(t.deleted);
      await holidays.reload();
    } catch (err) {
      toast(err instanceof ApiError ? errorText(err) : t.deleteFailed, true);
    }
  }

  return (
    <>
      <TopBar title={t.title} />
      <div className="support-content" style={{ maxWidth: 760 }}>
        <p className="small secondary" role="status">
          {settings.data?.mode === "business-hours" ? t.intro : settings.data ? t.introAlways : null}
        </p>
        <SlaPolicies admin={admin} />
        <h2 style={{ fontSize: "1.05rem" }}>{t.holidaysTitle}</h2>
        {admin && (
          <form className="card row" onSubmit={add} aria-label={t.addForm}>
            <div className="field"><label htmlFor="h-date" className="required">{t.date}</label>
              <input id="h-date" type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} required /></div>
            <div className="field grow"><label htmlFor="h-label" className="required">{t.label}</label>
              <input id="h-label" className="input" value={label} maxLength={120} onChange={(e) => setLabel(e.target.value)} required /></div>
            <button type="submit" className="btn btn-primary" style={{ alignSelf: "flex-end" }} disabled={!date || !label.trim()}>{t.add}</button>
          </form>
        )}
        {holidays.loading && !holidays.data && <Loading />}
        {holidays.error && <ErrorState error={holidays.error} onRetry={holidays.reload} />}
        {holidays.data?.length === 0 && (
          <EmptyState icon="📅" title={t.empty}>{t.emptyHint}</EmptyState>
        )}
        {!!holidays.data?.length && (
          <ul className="list-links">
            {holidays.data.map((h) => (
              <li key={h.date} className="list-link">
                <span className="mono">{f.date(h.date)}</span>
                <span className="grow">{h.label}</span>
                {admin && <button type="button" className="btn btn-tertiary btn-sm" onClick={() => remove(h)}>{t.remove}</button>}
              </li>
            ))}
          </ul>
        )}
      </div>
    </>
  );
}

/** SLA targets per priority (DEC-CS11-02: one row per priority of the vocabulary). */
function SlaPolicies({ admin }: { admin: boolean }) {
  const t = useMessages(adminCalendar);
  const labels = useLabels();
  const errorText = useErrorText();
  const toast = useToast();
  const policies = useApi<SlaPolicy[]>("/admin/sla-policies");
  const [edits, setEdits] = useState<Record<string, { first: string; value: string; unit: "minutes" | "days" }>>({});

  if (policies.error) return <ErrorState error={policies.error} onRetry={policies.reload} />;
  if (!policies.data) return <Loading />;

  const row = (p: SlaPolicy) => edits[p.priorityCode] ?? {
    first: String(p.firstResponseMinutes),
    value: String(p.resolutionDays ?? p.resolutionMinutes ?? ""),
    unit: p.resolutionDays != null ? "days" as const : "minutes" as const,
  };

  async function save(p: SlaPolicy) {
    const r = row(p);
    try {
      await api.put(`/admin/sla-policies/${p.priorityCode}`, {
        firstResponseMinutes: Number(r.first),
        resolutionMinutes: r.unit === "minutes" ? Number(r.value) : null,
        resolutionDays: r.unit === "days" ? Number(r.value) : null,
      });
      toast(format(t.policySaved, { priority: labels.priority[p.priorityCode] }));
      setEdits(({ [p.priorityCode]: _, ...rest }) => rest);
      await policies.reload();
    } catch (err) {
      toast(err instanceof ApiError ? errorText(err) : t.saveFailed, true);
    }
  }

  return (
    <section className="card stack-sm" aria-labelledby="policies-title">
      <h2 id="policies-title" style={{ fontSize: "1.05rem" }}>{t.policiesTitle}</h2>
      <p className="small muted">{t.policiesIntro}</p>
      <div className="table-wrap">
        <table className="data">
          <thead><tr><th scope="col">{t.colPriority}</th><th scope="col">{t.colFirstResponse}</th><th scope="col">{t.colResolution}</th>{admin && <th scope="col" />}</tr></thead>
          <tbody>
            {policies.data.map((p) => {
              const r = row(p);
              const set = (patch: Partial<typeof r>) => setEdits({ ...edits, [p.priorityCode]: { ...r, ...patch } });
              return (
                <tr key={p.priorityCode}>
                  <th scope="row">{labels.priority[p.priorityCode]}</th>
                  <td>
                    <label className="sr-only" htmlFor={`fr-${p.priorityCode}`}>{t.colFirstResponse}</label>
                    <input id={`fr-${p.priorityCode}`} className="input" type="number" min={1} max={43200} value={r.first}
                           disabled={!admin} onChange={(e) => set({ first: e.target.value })} style={{ maxWidth: 110 }} />
                  </td>
                  <td>
                    <span className="row" style={{ gap: 6 }}>
                      <label className="sr-only" htmlFor={`res-${p.priorityCode}`}>{t.colResolution}</label>
                      <input id={`res-${p.priorityCode}`} className="input" type="number" min={1} value={r.value}
                             disabled={!admin} onChange={(e) => set({ value: e.target.value })} style={{ maxWidth: 110 }} />
                      <label className="sr-only" htmlFor={`unit-${p.priorityCode}`}>{t.colResolution}</label>
                      <select id={`unit-${p.priorityCode}`} className="select" value={r.unit} disabled={!admin}
                              onChange={(e) => set({ unit: e.target.value as "minutes" | "days" })}>
                        <option value="minutes">{t.unitMinutes}</option>
                        <option value="days">{t.unitDays}</option>
                      </select>
                    </span>
                  </td>
                  {admin && (
                    <td>
                      <button type="button" className="btn btn-secondary btn-sm" disabled={!edits[p.priorityCode] || !r.first || !r.value}
                              onClick={() => save(p)}>{t.save}</button>
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
