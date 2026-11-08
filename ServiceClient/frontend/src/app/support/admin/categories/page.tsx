"use client";

import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { useApi } from "@/lib/useApi";
import { useErrorText, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { adminCategories } from "@/lib/i18n/messages/admin";
import { common } from "@/lib/i18n/messages/common";
import type { Category } from "@/lib/types";
import { TopBar } from "@/components/SupportShell";
import { useSession } from "@/components/session";
import { ErrorState, Loading, useToast } from "@/components/ui";

/** Ticket categories (amended BR-006): administrators add, rename, translate and deactivate them. */
export default function CategoriesPage() {
  const t = useMessages(adminCategories);
  const c = useMessages(common);
  const errorText = useErrorText();
  const session = useSession();
  const toast = useToast();
  const categories = useApi<Category[]>("/admin/categories");
  const [editing, setEditing] = useState<Category | null>(null);
  const [creating, setCreating] = useState<{ code: string; name: string; nameEn: string; description: string; descriptionEn: string } | null>(null);
  const admin = session.role === "ADMINISTRATOR";

  async function save(e: React.FormEvent) {
    e.preventDefault();
    if (!editing) return;
    try {
      await api.put(`/admin/categories/${editing.code}`, {
        name: editing.name, nameEn: editing.nameEn || null, description: editing.description,
        descriptionEn: editing.descriptionEn ?? "", sortOrder: editing.sortOrder ?? null, active: editing.active,
      });
      toast(t.saved);
      setEditing(null);
      await categories.reload();
    } catch (err) {
      toast(err instanceof ApiError ? errorText(err) : t.saveFailed, true);
    }
  }

  async function create(e: React.FormEvent) {
    e.preventDefault();
    if (!creating) return;
    try {
      await api.post("/admin/categories", {
        code: creating.code.trim().toUpperCase(), name: creating.name.trim(),
        nameEn: creating.nameEn.trim() || null, description: creating.description.trim() || null,
        descriptionEn: creating.descriptionEn.trim() || null,
      });
      toast(t.created);
      setCreating(null);
      await categories.reload();
    } catch (err) {
      toast(err instanceof ApiError ? errorText(err) : t.saveFailed, true);
    }
  }

  return (
    <>
      <TopBar title={t.title}>
        {admin && !creating && (
          <button type="button" className="btn btn-primary btn-sm"
                  onClick={() => setCreating({ code: "", name: "", nameEn: "", description: "", descriptionEn: "" })}>{t.create}</button>
        )}
      </TopBar>
      <div className="support-content" style={{ maxWidth: 900 }}>
        <p className="small muted">{t.intro}</p>
        {categories.loading && !categories.data && <Loading />}
        {categories.error && <ErrorState error={categories.error} onRetry={categories.reload} />}
        {categories.data && (
          <div className="table-wrap">
            <table className="data">
              <thead><tr><th scope="col">{t.colCode}</th><th scope="col">{t.colName}</th><th scope="col">{t.colDescription}</th><th scope="col">{t.colStatus}</th>{admin && <th scope="col"><span className="sr-only">{t.colActions}</span></th>}</tr></thead>
              <tbody>
                {categories.data.map((cat) => (
                  <tr key={cat.code}>
                    <td className="mono">{cat.code}</td>
                    <td><strong>{cat.name}</strong>{cat.nameEn && <div className="xsmall muted">{cat.nameEn}</div>}</td>
                    <td className="small secondary">{cat.description}{cat.descriptionEn && <div className="xsmall muted">{cat.descriptionEn}</div>}</td>
                    <td><span className={`badge ${cat.active ? "tone-success" : "tone-neutral"}`}>{cat.active ? t.active : t.inactive}</span></td>
                    {admin && <td><button type="button" className="btn btn-secondary btn-sm" onClick={() => setEditing({ ...cat })}>{t.edit}</button></td>}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {creating && (
          <form className="card stack-sm" onSubmit={create} aria-label={t.createTitle}>
            <h2>{t.createTitle}</h2>
            <div className="field"><label htmlFor="n-code" className="required">{t.code}</label>
              <input id="n-code" className="input mono" value={creating.code} maxLength={30} pattern="[A-Za-z0-9_]{2,30}"
                     onChange={(e) => setCreating({ ...creating, code: e.target.value.toUpperCase() })} /></div>
            <div className="field"><label htmlFor="n-name" className="required">{t.name}</label>
              <input id="n-name" className="input" value={creating.name} maxLength={80} onChange={(e) => setCreating({ ...creating, name: e.target.value })} /></div>
            <div className="field"><label htmlFor="n-name-en">{t.nameEn}</label>
              <input id="n-name-en" className="input" value={creating.nameEn} maxLength={80} onChange={(e) => setCreating({ ...creating, nameEn: e.target.value })} /></div>
            <div className="field"><label htmlFor="n-desc">{t.description}</label>
              <input id="n-desc" className="input" value={creating.description} maxLength={255} onChange={(e) => setCreating({ ...creating, description: e.target.value })} /></div>
            <div className="field"><label htmlFor="n-desc-en">{t.descriptionEn}</label>
              <input id="n-desc-en" className="input" value={creating.descriptionEn} maxLength={255} onChange={(e) => setCreating({ ...creating, descriptionEn: e.target.value })} /></div>
            <div className="row"><button type="submit" className="btn btn-primary" disabled={!creating.code.trim() || !creating.name.trim()}>{c.save}</button>
              <button type="button" className="btn btn-tertiary" onClick={() => setCreating(null)}>{c.cancel}</button></div>
          </form>
        )}
        {editing && (
          <form className="card stack-sm" onSubmit={save} aria-label={format(t.editLabel, { code: editing.code })}>
            <h2>{format(t.editTitle, { code: editing.code })}</h2>
            <div className="field"><label htmlFor="c-name" className="required">{t.name}</label>
              <input id="c-name" className="input" value={editing.name} maxLength={80} onChange={(e) => setEditing({ ...editing, name: e.target.value })} /></div>
            <div className="field"><label htmlFor="c-name-en">{t.nameEn}</label>
              <input id="c-name-en" className="input" value={editing.nameEn ?? ""} maxLength={80} onChange={(e) => setEditing({ ...editing, nameEn: e.target.value })} /></div>
            <div className="field"><label htmlFor="c-desc">{t.description}</label>
              <input id="c-desc" className="input" value={editing.description ?? ""} maxLength={255} onChange={(e) => setEditing({ ...editing, description: e.target.value })} /></div>
            <div className="field"><label htmlFor="c-desc-en">{t.descriptionEn}</label>
              <input id="c-desc-en" className="input" value={editing.descriptionEn ?? ""} maxLength={255} onChange={(e) => setEditing({ ...editing, descriptionEn: e.target.value })} /></div>
            <label className="checkbox"><input type="checkbox" checked={editing.active} onChange={(e) => setEditing({ ...editing, active: e.target.checked })} /> {t.activeCheckbox}</label>
            <div className="row"><button type="submit" className="btn btn-primary" disabled={!editing.name.trim()}>{c.save}</button>
              <button type="button" className="btn btn-tertiary" onClick={() => setEditing(null)}>{c.cancel}</button></div>
          </form>
        )}
      </div>
    </>
  );
}
