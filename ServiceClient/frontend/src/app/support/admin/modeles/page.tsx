"use client";

import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { useErrorText, useLocale, useMessages } from "@/lib/i18n/client";
import { adminTemplates } from "@/lib/i18n/messages/adminSettings";
import { common } from "@/lib/i18n/messages/common";
import { useApi } from "@/lib/useApi";
import type { CannedReply, Category, MessageTemplate } from "@/lib/types";
import { TopBar } from "@/components/SupportShell";
import { useSession } from "@/components/session";
import { ErrorState, Loading, useToast } from "@/components/ui";

type Draft = { id: string | null; title: string; body: string; language: "fr" | "en"; categoryCode: string; active: boolean };

/** Canned replies (managers) and the texts of the notifications (administrators), FR and EN. */
export default function TemplatesPage() {
  const t = useMessages(adminTemplates);
  return (
    <>
      <TopBar title={t.title} />
      <div className="support-content stack" style={{ maxWidth: 1000 }}>
        <CannedReplies />
        <NotificationTexts />
      </div>
    </>
  );
}

function CannedReplies() {
  const t = useMessages(adminTemplates);
  const c = useMessages(common);
  const locale = useLocale();
  const toast = useToast();
  const errorText = useErrorText();
  const replies = useApi<CannedReply[]>("/canned-replies?all=true");
  const categories = useApi<Category[]>("/categories");
  const [draft, setDraft] = useState<Draft | null>(null);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    if (!draft) return;
    const body = { title: draft.title.trim(), body: draft.body.trim(), language: draft.language,
      categoryCode: draft.categoryCode || null, active: draft.active };
    try {
      if (draft.id) await api.put(`/canned-replies/${draft.id}`, body);
      else await api.post("/canned-replies", body);
      toast(t.saved);
      setDraft(null);
      await replies.reload();
    } catch (err) {
      toast(err instanceof ApiError ? errorText(err) : String(err), true);
    }
  }

  async function remove(r: CannedReply) {
    try {
      await api.del(`/canned-replies/${r.id}`);
      toast(t.removed);
      await replies.reload();
    } catch (err) {
      toast(err instanceof ApiError ? errorText(err) : String(err), true);
    }
  }

  const categoryName = (code: string | null) => {
    if (!code) return t.allCategories;
    const cat = categories.data?.find((x) => x.code === code);
    return (cat && ((locale === "en" && cat.nameEn) || cat.name)) || code;
  };

  return (
    <section className="card stack-sm" aria-labelledby="canned-title">
      <div className="row-between">
        <h2 id="canned-title">{t.cannedTitle}</h2>
        {!draft && (
          <button type="button" className="btn btn-primary btn-sm"
                  onClick={() => setDraft({ id: null, title: "", body: "", language: locale, categoryCode: "", active: true })}>{t.add}</button>
        )}
      </div>
      <p className="small muted">{t.cannedIntro}</p>
      {replies.error && <ErrorState error={replies.error} onRetry={replies.reload} />}
      {replies.loading && !replies.data && <Loading />}
      {!!replies.data?.length && (
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th scope="col">{t.colTitle}</th><th scope="col">{t.colLanguage}</th><th scope="col">{t.colCategory}</th>
              <th scope="col">{t.active}</th><th scope="col"><span className="sr-only">{t.edit}</span></th></tr></thead>
            <tbody>
              {replies.data.map((r) => (
                <tr key={r.id}>
                  <td><strong>{r.title}</strong><div className="xsmall muted pre-wrap">{r.body.slice(0, 140)}{r.body.length > 140 ? "…" : ""}</div></td>
                  <td>{r.language.toUpperCase()}</td>
                  <td className="small">{categoryName(r.categoryCode)}</td>
                  <td>{r.active ? "✓" : "—"}</td>
                  <td className="row">
                    <button type="button" className="btn btn-secondary btn-sm"
                            onClick={() => setDraft({ id: r.id, title: r.title, body: r.body, language: r.language, categoryCode: r.categoryCode ?? "", active: r.active })}>{t.edit}</button>
                    <button type="button" className="btn btn-tertiary btn-sm" onClick={() => remove(r)}>{t.remove}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {draft && (
        <form className="stack-sm" onSubmit={save}>
          <div className="field"><label htmlFor="cr-title" className="required">{t.colTitle}</label>
            <input id="cr-title" className="input" value={draft.title} maxLength={120} onChange={(e) => setDraft({ ...draft, title: e.target.value })} /></div>
          <div className="row">
            <div className="field"><label htmlFor="cr-lang">{t.colLanguage}</label>
              <select id="cr-lang" className="select" value={draft.language} onChange={(e) => setDraft({ ...draft, language: e.target.value as "fr" | "en" })}>
                <option value="fr">Français</option><option value="en">English</option>
              </select></div>
            <div className="field"><label htmlFor="cr-cat">{t.colCategory}</label>
              <select id="cr-cat" className="select" value={draft.categoryCode} onChange={(e) => setDraft({ ...draft, categoryCode: e.target.value })}>
                <option value="">{t.allCategories}</option>
                {categories.data?.map((x) => <option key={x.code} value={x.code}>{categoryName(x.code)}</option>)}
              </select></div>
            <label className="checkbox" style={{ alignSelf: "flex-end" }}>
              <input type="checkbox" checked={draft.active} onChange={(e) => setDraft({ ...draft, active: e.target.checked })} /> {t.active}
            </label>
          </div>
          <div className="field"><label htmlFor="cr-body" className="required">{t.body}</label>
            <textarea id="cr-body" className="textarea" rows={5} value={draft.body} maxLength={5000} onChange={(e) => setDraft({ ...draft, body: e.target.value })} /></div>
          <div className="row">
            <button type="submit" className="btn btn-primary" disabled={!draft.title.trim() || !draft.body.trim()}>{c.save}</button>
            <button type="button" className="btn btn-tertiary" onClick={() => setDraft(null)}>{c.cancel}</button>
          </div>
        </form>
      )}
    </section>
  );
}

function NotificationTexts() {
  const t = useMessages(adminTemplates);
  const c = useMessages(common);
  const session = useSession();
  const toast = useToast();
  const errorText = useErrorText();
  const templates = useApi<MessageTemplate[]>("/admin/notification-templates");
  const [edits, setEdits] = useState<Record<string, { title: string; body: string }>>({});
  const admin = session.role === "ADMINISTRATOR";

  if (templates.error) return <ErrorState error={templates.error} onRetry={templates.reload} />;
  if (!templates.data) return <Loading />;

  async function save(m: MessageTemplate) {
    const k = `${m.type}/${m.language}`;
    try {
      await api.put(`/admin/notification-templates/${k}`, edits[k]);
      toast(t.templateSaved);
      setEdits(({ [k]: _, ...rest }) => rest);
      await templates.reload();
    } catch (err) {
      toast(err instanceof ApiError ? errorText(err) : String(err), true);
    }
  }

  const types = [...new Set(templates.data.map((m) => m.type))];
  return (
    <section className="card stack-sm" aria-labelledby="templates-title">
      <h2 id="templates-title">{t.templatesTitle}</h2>
      <p className="small muted">{t.templatesIntro}</p>
      {types.map((type) => (
        <details key={type} className="stack-sm">
          <summary className="mono small">{type}</summary>
          {templates.data!.filter((m) => m.type === type).sort((a, b) => a.language.localeCompare(b.language) * -1).map((m) => {
            const k = `${m.type}/${m.language}`;
            const v = edits[k] ?? { title: m.title, body: m.body };
            return (
              <div key={k} className="stack-sm" style={{ paddingLeft: 12 }}>
                <strong className="small">{m.language === "fr" ? "Français" : "English"}</strong>
                <div className="field"><label htmlFor={`t-${k}`} className="small">{t.titleField}</label>
                  <input id={`t-${k}`} className="input" value={v.title} maxLength={160} disabled={!admin}
                         onChange={(e) => setEdits({ ...edits, [k]: { ...v, title: e.target.value } })} /></div>
                <div className="field"><label htmlFor={`b-${k}`} className="small">{t.bodyField}</label>
                  <textarea id={`b-${k}`} className="textarea" rows={2} value={v.body} maxLength={500} disabled={!admin}
                            onChange={(e) => setEdits({ ...edits, [k]: { ...v, body: e.target.value } })} /></div>
                {admin && edits[k] && (
                  <div className="row">
                    <button type="button" className="btn btn-primary btn-sm" disabled={!v.title.trim() || !v.body.trim()} onClick={() => save(m)}>{c.save}</button>
                    <button type="button" className="btn btn-tertiary btn-sm" onClick={() => setEdits(({ [k]: _, ...rest }) => rest)}>{c.cancel}</button>
                  </div>
                )}
              </div>
            );
          })}
        </details>
      ))}
    </section>
  );
}
