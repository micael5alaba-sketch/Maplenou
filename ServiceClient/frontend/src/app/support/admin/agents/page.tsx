"use client";

import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { useErrorText, useFormat, useLabels, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { adminAgents } from "@/lib/i18n/messages/admin";
import { useApi } from "@/lib/useApi";
import type { Agent, Role } from "@/lib/types";
import { TopBar } from "@/components/SupportShell";
import { useSession } from "@/components/session";
import { Avatar, EmptyState, ErrorState, Loading, useToast } from "@/components/ui";

interface StaffRole {
  phone: string;
  role: Role;
  assignedAt: string;
}

const STAFF_ROLES: Role[] = ["SUPPORT_AGENT", "SUPPORT_MANAGER", "ADMINISTRATOR"];

/** Support agents and workload. Activation is an administrator action (BR-065); roles come from the Maplenou account. */
export default function AgentsPage() {
  const t = useMessages(adminAgents);
  const labels = useLabels();
  const errorText = useErrorText();
  const session = useSession();
  const toast = useToast();
  const agents = useApi<Agent[]>("/admin/agents");
  const admin = session.role === "ADMINISTRATOR";

  async function toggle(a: Agent) {
    try {
      await api.put(`/admin/agents/${a.id}/active`, { active: !a.active });
      toast(format(a.active ? t.deactivatedToast : t.reactivatedToast, { name: a.name }));
      await agents.reload();
    } catch (e) {
      toast(e instanceof ApiError ? errorText(e) : t.actionFailed, true);
    }
  }

  return (
    <>
      <TopBar title={t.title} />
      <div className="support-content">
        <p className="small muted">{t.intro}</p>
        {agents.loading && !agents.data && <Loading />}
        {agents.error && <ErrorState error={agents.error} onRetry={agents.reload} />}
        {agents.data && (
          <div className="table-wrap">
            <table className="data">
              <thead><tr><th scope="col">{t.colAgent}</th><th scope="col">{t.colRole}</th><th scope="col">{t.colOpenTickets}</th><th scope="col">{t.colStatus}</th>{admin && <th scope="col"><span className="sr-only">{t.colActions}</span></th>}</tr></thead>
              <tbody>
                {agents.data.map((a) => (
                  <tr key={a.id}>
                    <td><div className="row"><Avatar name={a.name} small /><span><strong>{a.name}</strong><br /><span className="xsmall muted">{a.email ?? ""}</span></span></div></td>
                    <td>{labels.role[a.role]}</td>
                    <td className="mono">{a.openTickets}</td>
                    <td><span className={`badge ${a.active ? "tone-success" : "tone-neutral"}`}>{a.active ? t.active : t.deactivated}</span></td>
                    {admin && (
                      <td style={{ textAlign: "right" }}>
                        {a.id !== session.id && (
                          <button type="button" className="btn btn-secondary btn-sm" onClick={() => toggle(a)}>
                            {a.active ? t.deactivate : t.reactivate}
                          </button>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {admin && <StaffRoles />}
        {admin && <LegacyImport />}
      </div>
    </>
  );
}

/** Support roles given to Maplenou accounts by phone number (DEC-CS10-17). */
function StaffRoles() {
  const t = useMessages(adminAgents);
  const labels = useLabels();
  const f = useFormat();
  const errorText = useErrorText();
  const toast = useToast();
  const list = useApi<StaffRole[]>("/admin/staff-roles");
  const [phone, setPhone] = useState("");
  const [role, setRole] = useState<Role>("SUPPORT_AGENT");

  async function save(e: React.FormEvent) {
    e.preventDefault();
    try {
      const saved = await api.put<StaffRole>("/admin/staff-roles", { phone, role });
      toast(format(t.staffSaved, { phone: saved.phone }));
      setPhone("");
      await list.reload();
    } catch (err) {
      toast(err instanceof ApiError ? errorText(err) : t.actionFailed, true);
    }
  }

  async function remove(s: StaffRole) {
    try {
      await api.del(`/admin/staff-roles/${encodeURIComponent(s.phone)}`);
      toast(format(t.staffRemoved, { phone: s.phone }));
      await list.reload();
    } catch (err) {
      toast(err instanceof ApiError ? errorText(err) : t.actionFailed, true);
    }
  }

  return (
    <section className="card stack-sm" aria-labelledby="staff-title" style={{ marginTop: 24 }}>
      <h2 id="staff-title">{t.staffTitle}</h2>
      <p className="small secondary">{t.staffIntro}</p>
      <form className="row" onSubmit={save}>
        <div className="field grow"><label htmlFor="staff-phone" className="required">{t.staffPhone}</label>
          <input id="staff-phone" className="input" type="tel" required value={phone} onChange={(e) => setPhone(e.target.value)} /></div>
        <div className="field"><label htmlFor="staff-role">{t.staffRole}</label>
          <select id="staff-role" className="input" value={role} onChange={(e) => setRole(e.target.value as Role)}>
            {STAFF_ROLES.map((r) => <option key={r} value={r}>{labels.role[r]}</option>)}
          </select></div>
        <button type="submit" className="btn btn-primary btn-sm" style={{ alignSelf: "flex-end" }}>{t.staffSave}</button>
      </form>
      {list.error && <ErrorState error={list.error} onRetry={list.reload} />}
      {list.data?.length === 0 && <EmptyState title={t.staffEmpty} />}
      {list.data && list.data.length > 0 && (
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th scope="col">{t.staffPhone}</th><th scope="col">{t.staffRole}</th><th scope="col">{t.staffAssigned}</th><th scope="col"><span className="sr-only">{t.colActions}</span></th></tr></thead>
            <tbody>
              {list.data.map((s) => (
                <tr key={s.phone}>
                  <td className="mono">{s.phone}</td>
                  <td>{labels.role[s.role]}</td>
                  <td>{f.dateTime(s.assignedAt)}</td>
                  <td style={{ textAlign: "right" }}>
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => remove(s)}>{t.staffRemove}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

/** Import of the former Maplenou support tickets (Bakend V46); safe to run again, also scheduled (DEC-CS12-02). */
function LegacyImport() {
  const t = useMessages(adminAgents);
  const errorText = useErrorText();
  const toast = useToast();
  const [busy, setBusy] = useState(false);

  async function run() {
    setBusy(true);
    try {
      const r = await api.post<{ imported: number; skipped: number; updated: number }>("/admin/legacy-import");
      toast(format(t.legacyDone, { imported: r.imported, skipped: r.skipped, updated: r.updated }));
    } catch (err) {
      toast(err instanceof ApiError ? errorText(err) : t.actionFailed, true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card stack-sm" aria-labelledby="legacy-title" style={{ marginTop: 16 }}>
      <h2 id="legacy-title">{t.legacyTitle}</h2>
      <p className="small secondary">{t.legacyIntro}</p>
      <div><button type="button" className="btn btn-secondary btn-sm" onClick={run} disabled={busy}>{t.legacyRun}</button></div>
    </section>
  );
}
