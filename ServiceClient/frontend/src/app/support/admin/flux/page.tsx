"use client";

import { useMemo, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { useErrorText, useLocale, useMessages } from "@/lib/i18n/client";
import { adminWorkflow } from "@/lib/i18n/messages/adminSettings";
import { common } from "@/lib/i18n/messages/common";
import { useApi } from "@/lib/useApi";
import type { PriorityDefinition, StatusDefinition, StatusKind, Vocabulary } from "@/lib/types";
import { TopBar } from "@/components/SupportShell";
import { useSession } from "@/components/session";
import { Alert, ErrorState, Loading, useToast } from "@/components/ui";

const CUSTOM_KINDS: StatusKind[] = ["IN_PROGRESS", "ON_HOLD"];
const CODE = /^[A-Z][A-Z0-9_]{1,24}$/;

/** Configurable vocabulary (DEC-CS11-02): statuses, manual transitions and priorities. */
export default function WorkflowPage() {
  const t = useMessages(adminWorkflow);
  const session = useSession();
  const vocabulary = useApi<Vocabulary>("/vocabulary");
  const admin = session.role === "ADMINISTRATOR";
  return (
    <>
      <TopBar title={t.title} />
      <div className="support-content stack" style={{ maxWidth: 1100 }}>
        <p className="small secondary">{t.intro}</p>
        {!admin && <Alert tone="info" title={t.readOnly} />}
        {vocabulary.error && <ErrorState error={vocabulary.error} onRetry={vocabulary.reload} />}
        {!vocabulary.data && !vocabulary.error && <Loading lines={6} />}
        {vocabulary.data && (
          <>
            <Statuses vocabulary={vocabulary.data} admin={admin} onSaved={vocabulary.reload} />
            <Priorities vocabulary={vocabulary.data} admin={admin} onSaved={vocabulary.reload} />
          </>
        )}
      </div>
    </>
  );
}

function Statuses({ vocabulary, admin, onSaved }: { vocabulary: Vocabulary; admin: boolean; onSaved: () => Promise<void> }) {
  const t = useMessages(adminWorkflow);
  const toast = useToast();
  const errorText = useErrorText();
  const [statuses, setStatuses] = useState<StatusDefinition[]>(() =>
    [...vocabulary.statuses].sort((a, b) => a.sortOrder - b.sortOrder));
  const [moves, setMoves] = useState<Set<string>>(() => new Set(vocabulary.transitions.map((m) => `${m.from}>${m.to}`)));
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const kinds = t as unknown as Record<string, string>;
  const locale = useLocale();
  const active = useMemo(() => statuses.filter((s) => s.active), [statuses]);
  const label = (s: StatusDefinition) => (locale === "en" ? s.labelEn : s.labelFr) || s.code;

  const update = (i: number, patch: Partial<StatusDefinition>) =>
    setStatuses(statuses.map((s, j) => (j === i ? { ...s, ...patch } : s)));
  const toggle = (from: string, to: string) => {
    const next = new Set(moves);
    const k = `${from}>${to}`;
    if (next.has(k)) next.delete(k);
    else next.add(k);
    setMoves(next);
  };

  function add() {
    const c = code.trim().toUpperCase();
    if (!CODE.test(c) || statuses.some((s) => s.code === c)) return;
    const order = Math.max(...statuses.map((s) => s.sortOrder)) + 10;
    setStatuses([...statuses, { code: c, kind: "ON_HOLD", labelFr: c, labelEn: c, sortOrder: order, slaPaused: true, system: false, active: true }]);
    setCode("");
  }

  async function save() {
    setBusy(true);
    try {
      await api.put("/admin/workflow", {
        statuses: statuses.map(({ code: c, kind, labelFr, labelEn, sortOrder, slaPaused, active }) =>
          ({ code: c, kind, labelFr, labelEn: labelEn || null, sortOrder, slaPaused, active })),
        transitions: [...moves].map((m) => { const [from, to] = m.split(">"); return { from, to }; }),
      });
      toast(t.workflowSaved);
      await onSaved();
    } catch (e) {
      toast(e instanceof ApiError ? errorText(e) : String(e), true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card stack-sm" aria-labelledby="statuses-title">
      <h2 id="statuses-title">{t.statuses}</h2>
      <div className="table-wrap">
        <table className="data">
          <thead><tr>
            <th scope="col">{t.colCode}</th><th scope="col">{t.colKind}</th><th scope="col">{t.colLabelFr}</th>
            <th scope="col">{t.colLabelEn}</th><th scope="col">{t.colOrder}</th><th scope="col">{t.colPause}</th>
            <th scope="col">{t.colActive}</th><th scope="col"><span className="sr-only">{t.remove}</span></th>
          </tr></thead>
          <tbody>
            {statuses.map((s, i) => (
              <tr key={s.code}>
                <th scope="row" className="mono small">{s.code}{s.system && <div className="xsmall muted">{t.system}</div>}</th>
                <td>
                  {s.system ? <span className="small">{kinds[`kind${s.kind}`]}</span> : (
                    <select className="select" aria-label={`${t.colKind} ${s.code}`} value={s.kind} disabled={!admin}
                            onChange={(e) => update(i, { kind: e.target.value as StatusKind })}>
                      {CUSTOM_KINDS.map((k) => <option key={k} value={k}>{kinds[`kind${k}`]}</option>)}
                    </select>
                  )}
                </td>
                <td><input className="input" aria-label={`${t.colLabelFr} ${s.code}`} value={s.labelFr} maxLength={60} disabled={!admin}
                           onChange={(e) => update(i, { labelFr: e.target.value })} /></td>
                <td><input className="input" aria-label={`${t.colLabelEn} ${s.code}`} value={s.labelEn ?? ""} maxLength={60} disabled={!admin}
                           onChange={(e) => update(i, { labelEn: e.target.value })} /></td>
                <td><input className="input" type="number" aria-label={`${t.colOrder} ${s.code}`} value={s.sortOrder} min={0} max={999}
                           style={{ maxWidth: 80 }} disabled={!admin} onChange={(e) => update(i, { sortOrder: Number(e.target.value) })} /></td>
                <td><input type="checkbox" aria-label={`${t.colPause} ${s.code}`} checked={s.slaPaused} disabled={!admin || s.kind === "RESOLVED" || s.kind === "CLOSED"}
                           onChange={(e) => update(i, { slaPaused: e.target.checked })} /></td>
                <td><input type="checkbox" aria-label={`${t.colActive} ${s.code}`} checked={s.active} disabled={!admin || s.system}
                           onChange={(e) => update(i, { active: e.target.checked })} /></td>
                <td>{!s.system && admin && (
                  <button type="button" className="btn btn-tertiary btn-sm" onClick={() => setStatuses(statuses.filter((_, j) => j !== i))}>{t.remove}</button>
                )}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {admin && (
        <div className="row">
          <label className="sr-only" htmlFor="new-status">{t.newCode}</label>
          <input id="new-status" className="input mono" placeholder={t.newCode} value={code} maxLength={25}
                 onChange={(e) => setCode(e.target.value.toUpperCase())} />
          <button type="button" className="btn btn-secondary btn-sm" disabled={!CODE.test(code)} onClick={add}>{t.addStatus}</button>
        </div>
      )}

      <h3 className="panel-title">{t.transitions}</h3>
      <p className="xsmall muted">{t.transitionsHint}</p>
      <div className="table-wrap">
        <table className="data">
          <thead><tr><th scope="col">{t.from}</th>
            {active.map((s) => <th key={s.code} scope="col" className="xsmall">{label(s)}</th>)}</tr></thead>
          <tbody>
            {active.map((from) => (
              <tr key={from.code}>
                <th scope="row" className="xsmall">{label(from)}</th>
                {active.map((to) => (
                  <td key={to.code} style={{ textAlign: "center" }}>
                    {from.code !== to.code && (
                      <input type="checkbox" aria-label={`${label(from)} → ${label(to)}`} disabled={!admin}
                             checked={moves.has(`${from.code}>${to.code}`)} onChange={() => toggle(from.code, to.code)} />
                    )}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {admin && <div><button type="button" className="btn btn-primary" disabled={busy} onClick={save}>{t.saveWorkflow}</button></div>}
    </section>
  );
}

function Priorities({ vocabulary, admin, onSaved }: { vocabulary: Vocabulary; admin: boolean; onSaved: () => Promise<void> }) {
  const t = useMessages(adminWorkflow);
  const c = useMessages(common);
  const toast = useToast();
  const errorText = useErrorText();
  const initial = useMemo(() => [...vocabulary.priorities].sort((a, b) => b.rank - a.rank), [vocabulary]);
  const [rows, setRows] = useState<PriorityDefinition[]>(initial);
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const update = (i: number, patch: Partial<PriorityDefinition>) => setRows(rows.map((p, j) => (j === i ? { ...p, ...patch } : p)));

  async function save() {
    setBusy(true);
    try {
      await api.put("/admin/priorities", rows.map(({ code: k, rank, labelFr, labelEn, requesterSelectable, defaultPriority, active }) =>
        ({ code: k, rank, labelFr, labelEn: labelEn || null, requesterSelectable, defaultPriority, active })));
      toast(t.prioritiesSaved);
      await onSaved();
    } catch (e) {
      toast(e instanceof ApiError ? errorText(e) : String(e), true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card stack-sm" aria-labelledby="priorities-title">
      <h2 id="priorities-title">{t.priorities}</h2>
      <div className="table-wrap">
        <table className="data">
          <thead><tr>
            <th scope="col">{t.colCode}</th><th scope="col">{t.colRank}</th><th scope="col">{t.colLabelFr}</th>
            <th scope="col">{t.colLabelEn}</th><th scope="col">{t.colRequester}</th><th scope="col">{t.colDefault}</th>
            <th scope="col">{t.colActive}</th>
          </tr></thead>
          <tbody>
            {rows.map((p, i) => (
              <tr key={p.code}>
                <th scope="row" className="mono small">{p.code}{p.system && <div className="xsmall muted">{t.system}</div>}</th>
                <td><input className="input" type="number" min={1} max={99} style={{ maxWidth: 80 }} aria-label={`${t.colRank} ${p.code}`}
                           value={p.rank} disabled={!admin} onChange={(e) => update(i, { rank: Number(e.target.value) })} /></td>
                <td><input className="input" aria-label={`${t.colLabelFr} ${p.code}`} value={p.labelFr} maxLength={40} disabled={!admin}
                           onChange={(e) => update(i, { labelFr: e.target.value })} /></td>
                <td><input className="input" aria-label={`${t.colLabelEn} ${p.code}`} value={p.labelEn ?? ""} maxLength={40} disabled={!admin}
                           onChange={(e) => update(i, { labelEn: e.target.value })} /></td>
                <td><input type="checkbox" aria-label={`${t.colRequester} ${p.code}`} checked={p.requesterSelectable}
                           disabled={!admin || p.code === "CRITICAL"} onChange={(e) => update(i, { requesterSelectable: e.target.checked })} /></td>
                <td><input type="radio" name="default-priority" aria-label={`${t.colDefault} ${p.code}`} checked={p.defaultPriority} disabled={!admin}
                           onChange={() => setRows(rows.map((r, j) => ({ ...r, defaultPriority: j === i })))} /></td>
                <td><input type="checkbox" aria-label={`${t.colActive} ${p.code}`} checked={p.active} disabled={!admin || p.system}
                           onChange={(e) => update(i, { active: e.target.checked })} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {admin && (
        <div className="row">
          <label className="sr-only" htmlFor="new-priority">{t.newPriority}</label>
          <input id="new-priority" className="input mono" placeholder={t.newPriority} value={code} maxLength={20}
                 onChange={(e) => setCode(e.target.value.toUpperCase())} />
          <button type="button" className="btn btn-secondary btn-sm" disabled={!CODE.test(code) || rows.some((r) => r.code === code)}
                  onClick={() => {
                    setRows([...rows, { code, rank: Math.max(...rows.map((r) => r.rank)) + 1, labelFr: code, labelEn: code,
                      requesterSelectable: true, defaultPriority: false, system: false, active: true }]);
                    setCode("");
                  }}>{t.addPriority}</button>
          <span className="grow" />
          <button type="button" className="btn btn-tertiary btn-sm" onClick={() => setRows(initial)}>{c.cancel}</button>
          <button type="button" className="btn btn-primary" disabled={busy} onClick={save}>{t.savePriorities}</button>
        </div>
      )}
    </section>
  );
}
