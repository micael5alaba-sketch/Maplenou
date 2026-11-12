"use client";

import { useEffect, useMemo, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { useErrorText, useLabels, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { adminPermissions } from "@/lib/i18n/messages/adminSettings";
import { useApi } from "@/lib/useApi";
import type { PermissionRule, PermissionScope, Role, TicketAction } from "@/lib/types";
import { TopBar } from "@/components/SupportShell";
import { ErrorState, Loading, useToast } from "@/components/ui";

const ROLES: Role[] = ["CUSTOMER", "SELLER", "DELIVERY_DRIVER", "SUPPORT_AGENT", "SUPPORT_MANAGER", "ADMINISTRATOR"];
const SCOPES: PermissionScope[] = ["OWN", "ASSIGNED", "UNASSIGNED", "ANY"];
const key = (r: PermissionRule) => `${r.role}|${r.action}|${r.scope}`;

/** Permission matrix as data (DEC-CS11-02), administrators only; the backend refuses unsafe matrices. */
export default function PermissionsPage() {
  const t = useMessages(adminPermissions);
  const actionLabel = (a: string) => (t as unknown as Record<string, string>)[`action${a}`] ?? a;
  const labels = useLabels();
  const errorText = useErrorText();
  const toast = useToast();
  const data = useApi<{ rules: PermissionRule[]; defaults: PermissionRule[] }>("/admin/permissions");
  const [rules, setRules] = useState<Set<string> | null>(null);
  const [role, setRole] = useState<Role>("SUPPORT_AGENT");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- start editing from the saved matrix
    if (data.data && rules === null) setRules(new Set(data.data.rules.map(key)));
  }, [data.data, rules]);

  const actions = useMemo(() => {
    const all = new Set<TicketAction>();
    for (const r of [...(data.data?.defaults ?? []), ...(data.data?.rules ?? [])]) all.add(r.action);
    return [...all].sort();
  }, [data.data]);

  if (data.error) return <><TopBar title={t.title} /><div className="support-content"><ErrorState error={data.error} onRetry={data.reload} /></div></>;
  if (!data.data || !rules) return <><TopBar title={t.title} /><div className="support-content"><Loading lines={8} /></div></>;

  const saved = new Set(data.data.rules.map(key));
  const changes = [...rules].filter((k) => !saved.has(k)).length + [...saved].filter((k) => !rules.has(k)).length;
  const toggle = (k: string) => {
    const next = new Set(rules);
    if (next.has(k)) next.delete(k);
    else next.add(k);
    setRules(next);
  };

  async function save() {
    setBusy(true);
    try {
      const body = [...rules!].map((k) => { const [r, a, s] = k.split("|"); return { role: r, action: a, scope: s }; });
      const res = await api.put<{ rules: PermissionRule[] }>("/admin/permissions", body);
      setRules(new Set(res.rules.map(key)));
      toast(t.saved);
      await data.reload();
    } catch (e) {
      toast(e instanceof ApiError ? errorText(e) : String(e), true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <TopBar title={t.title} />
      <div className="support-content stack" style={{ maxWidth: 900 }}>
        <p className="small secondary">{t.intro}</p>
        <div className="segmented" role="radiogroup" aria-label={t.title}>
          {ROLES.map((r) => (
            <label key={r}>
              <input type="radio" name="perm-role" checked={role === r} onChange={() => setRole(r)} />
              {labels.role[r]}
            </label>
          ))}
        </div>
        <div className="table-wrap">
          <table className="data">
            <caption className="sr-only">{labels.role[role]}</caption>
            <thead><tr><th scope="col">{t.colAction}</th>{SCOPES.map((s) => <th key={s} scope="col">{t[`scope${s}`]}</th>)}</tr></thead>
            <tbody>
              {actions.map((a) => (
                <tr key={a}>
                  <th scope="row" className="small">{actionLabel(a)}<div className="xsmall muted mono">{a}</div></th>
                  {SCOPES.map((s) => {
                    const k = `${role}|${a}|${s}`;
                    return (
                      <td key={s} style={{ textAlign: "center" }}>
                        <input type="checkbox" aria-label={`${labels.role[role]} · ${actionLabel(a)} · ${t[`scope${s}`]}`}
                               checked={rules.has(k)} onChange={() => toggle(k)} />
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="row">
          {changes > 0 && <span className="small muted grow">{format(t.changed, { count: changes })}</span>}
          <button type="button" className="btn btn-tertiary btn-sm" onClick={() => setRules(new Set(data.data!.defaults.map(key)))}>{t.reset}</button>
          <button type="button" className="btn btn-primary" disabled={busy || changes === 0} onClick={save}>{t.save}</button>
        </div>
      </div>
    </>
  );
}
