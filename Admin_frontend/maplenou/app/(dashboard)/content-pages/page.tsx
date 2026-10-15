"use client";

import { useEffect, useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { ContentPageResponse, Page } from "@/lib/types";
import { Button, Card, EmptyState, ErrorState, LoadingState, PageHeader, Table, formatDate } from "@/components/ui";

export default function ContentPagesPage() {
  const [data, setData] = useState<Page<ContentPageResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<ContentPageResponse | null>(null);
  const [creating, setCreating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const [slug, setSlug] = useState("");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [published, setPublished] = useState(false);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<ContentPageResponse>>("/api/admin/pages", { size: 50 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  function openCreate() {
    setCreating(true);
    setEditing(null);
    setSlug("");
    setTitle("");
    setBody("");
    setPublished(false);
    setFormError(null);
  }

  function openEdit(p: ContentPageResponse) {
    setEditing(p);
    setCreating(false);
    setTitle(p.title);
    setBody(p.body);
    setPublished(p.published);
    setFormError(null);
  }

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setSaving(true);
    try {
      if (creating) {
        await apiClient.post("/api/admin/pages", { slug, title, body, published });
      } else if (editing) {
        await apiClient.put(`/api/admin/pages/${editing.id}`, { title, body, published });
      }
      setCreating(false);
      setEditing(null);
      load();
    } catch (err) {
      setFormError(err instanceof ApiException ? err.message : "Erreur lors de l'enregistrement.");
    } finally {
      setSaving(false);
    }
  }

  async function remove(id: string) {
    if (!confirm("Supprimer cette page ?")) return;
    try {
      await apiClient.delete(`/api/admin/pages/${id}`);
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    }
  }

  const formOpen = creating || editing !== null;

  return (
    <div>
      <PageHeader
        title="Pages de contenu (CMS)"
        subtitle="CGU, CGV, FAQ, à propos…"
        action={!formOpen ? <Button onClick={openCreate}>Nouvelle page</Button> : undefined}
      />

      {formOpen && (
        <Card className="mb-6">
          <h2 className="mb-3 text-sm font-semibold text-foreground">{creating ? "Nouvelle page" : `Modifier : ${editing?.slug}`}</h2>
          {formError && (
            <div className="mb-3">
              <ErrorState message={formError} />
            </div>
          )}
          <form onSubmit={handleSave} className="flex flex-col gap-3">
            <div className="flex gap-3">
              {creating && (
                <div className="flex-1">
                  <label className="mb-1 block text-xs font-medium text-muted">Slug</label>
                  <input
                    required
                    value={slug}
                    onChange={(e) => setSlug(e.target.value)}
                    placeholder="cgu"
                    className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
                  />
                </div>
              )}
              <div className="flex-1">
                <label className="mb-1 block text-xs font-medium text-muted">Titre</label>
                <input
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
                />
              </div>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted">Contenu</label>
              <textarea
                required
                value={body}
                onChange={(e) => setBody(e.target.value)}
                rows={8}
                className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
              />
            </div>
            <label className="flex items-center gap-2 text-sm text-foreground">
              <input type="checkbox" checked={published} onChange={(e) => setPublished(e.target.checked)} />
              Publiée
            </label>
            <div className="flex gap-2">
              <Button type="submit" disabled={saving}>
                Enregistrer
              </Button>
              <Button variant="secondary" onClick={() => { setCreating(false); setEditing(null); }}>
                Annuler
              </Button>
            </div>
          </form>
        </Card>
      )}

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <Card>
          {data.content.length === 0 ? (
            <EmptyState message="Aucune page." />
          ) : (
            <Table headers={["Slug", "Titre", "Statut", "Mise à jour", "Actions"]}>
              {data.content.map((p) => (
                <tr key={p.id}>
                  <td className="px-4 py-3 font-mono text-foreground">{p.slug}</td>
                  <td className="px-4 py-3 text-muted">{p.title}</td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                        p.published ? "bg-primary/10 text-primary" : "bg-muted/10 text-muted"
                      }`}
                    >
                      {p.published ? "Publiée" : "Brouillon"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(p.updatedAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <Button variant="secondary" onClick={() => openEdit(p)}>
                        Modifier
                      </Button>
                      <Button variant="danger" onClick={() => remove(p.id)}>
                        Supprimer
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </Table>
          )}
        </Card>
      )}
    </div>
  );
}
