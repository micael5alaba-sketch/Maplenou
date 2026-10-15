"use client";

import { Fragment, useEffect, useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { CategoryResponse } from "@/lib/types";
import { Button, Card, EmptyState, ErrorState, LoadingState, PageHeader, Table } from "@/components/ui";

export default function CategoriesPage() {
  const [roots, setRoots] = useState<CategoryResponse[] | null>(null);
  const [children, setChildren] = useState<Record<string, CategoryResponse[]>>({});
  const [expanded, setExpanded] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [parentId, setParentId] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<CategoryResponse[]>("/api/categories")
      .then(setRoots)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function toggleExpand(id: string) {
    if (expanded === id) {
      setExpanded(null);
      return;
    }
    setExpanded(id);
    if (!children[id]) {
      try {
        const subs = await apiClient.get<CategoryResponse[]>(`/api/categories/${id}/subcategories`);
        setChildren((prev) => ({ ...prev, [id]: subs }));
      } catch {
        setChildren((prev) => ({ ...prev, [id]: [] }));
      }
    }
  }

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      await apiClient.post("/api/categories", { name, parentId: parentId || null });
      setName("");
      setParentId("");
      setChildren({});
      load();
    } catch (err) {
      setFormError(err instanceof ApiException ? err.message : "Erreur lors de la création.");
    } finally {
      setSubmitting(false);
    }
  }

  async function toggleActive(cat: CategoryResponse) {
    setActingId(cat.id);
    try {
      await apiClient.patch(`/api/categories/${cat.id}`, { active: !cat.active });
      setChildren({});
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  async function remove(id: string) {
    if (!confirm("Désactiver cette catégorie ?")) return;
    setActingId(id);
    try {
      await apiClient.delete(`/api/categories/${id}`);
      setChildren({});
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  return (
    <div>
      <PageHeader title="Catégories" subtitle="Référentiel du catalogue, hiérarchie à 2 niveaux" />

      <Card className="mb-6 max-w-xl">
        <h2 className="mb-3 text-sm font-semibold text-foreground">Nouvelle catégorie</h2>
        {formError && (
          <div className="mb-3">
            <ErrorState message={formError} />
          </div>
        )}
        <form onSubmit={handleCreate} className="flex flex-wrap items-end gap-3">
          <div className="flex-1 min-w-[160px]">
            <label className="mb-1 block text-xs font-medium text-muted">Nom</label>
            <input
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            />
          </div>
          <div className="min-w-[200px]">
            <label className="mb-1 block text-xs font-medium text-muted">Catégorie parente (optionnel)</label>
            <select
              value={parentId}
              onChange={(e) => setParentId(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            >
              <option value="">— Racine —</option>
              {roots?.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </select>
          </div>
          <Button type="submit" disabled={submitting}>
            Ajouter
          </Button>
        </form>
      </Card>

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {roots && (
        <Card>
          {roots.length === 0 ? (
            <EmptyState message="Aucune catégorie." />
          ) : (
            <Table headers={["Catégorie", "Slug", "Statut", "Actions"]}>
              {roots.map((cat) => (
                <Fragment key={cat.id}>
                  <tr>
                    <td className="px-4 py-3 font-medium text-foreground">
                      <button onClick={() => toggleExpand(cat.id)} className="mr-2 text-muted">
                        {expanded === cat.id ? "▾" : "▸"}
                      </button>
                      {cat.name}
                    </td>
                    <td className="px-4 py-3 text-muted">{cat.slug}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                          cat.active ? "bg-primary/10 text-primary" : "bg-muted/10 text-muted"
                        }`}
                      >
                        {cat.active ? "Active" : "Inactive"}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2">
                        <Button variant="secondary" onClick={() => toggleActive(cat)} disabled={actingId === cat.id}>
                          {cat.active ? "Désactiver" : "Activer"}
                        </Button>
                        <Button variant="danger" onClick={() => remove(cat.id)} disabled={actingId === cat.id}>
                          Supprimer
                        </Button>
                      </div>
                    </td>
                  </tr>
                  {expanded === cat.id &&
                    (children[cat.id] ?? []).map((sub) => (
                      <tr key={sub.id} className="bg-input-fill/40">
                        <td className="px-4 py-3 pl-10 text-foreground">{sub.name}</td>
                        <td className="px-4 py-3 text-muted">{sub.slug}</td>
                        <td className="px-4 py-3">
                          <span
                            className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                              sub.active ? "bg-primary/10 text-primary" : "bg-muted/10 text-muted"
                            }`}
                          >
                            {sub.active ? "Active" : "Inactive"}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex gap-2">
                            <Button variant="secondary" onClick={() => toggleActive(sub)} disabled={actingId === sub.id}>
                              {sub.active ? "Désactiver" : "Activer"}
                            </Button>
                            <Button variant="danger" onClick={() => remove(sub.id)} disabled={actingId === sub.id}>
                              Supprimer
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                </Fragment>
              ))}
            </Table>
          )}
        </Card>
      )}
    </div>
  );
}
