"use client";

import { useEffect, useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { Page, PromoCodeResponse } from "@/lib/types";
import {
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  StatusBadge,
  Table,
  formatDate,
  formatFcfa,
} from "@/components/ui";

export default function PromoCodesPage() {
  const [data, setData] = useState<Page<PromoCodeResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  const [code, setCode] = useState("");
  const [discountPercent, setDiscountPercent] = useState("");
  const [minOrderAmount, setMinOrderAmount] = useState("");
  const [maxUses, setMaxUses] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<PromoCodeResponse>>("/api/promo-codes/platform", { size: 50 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      await apiClient.post("/api/promo-codes", {
        code: code.toUpperCase(),
        scopeType: "PLATFORM",
        scopeId: null,
        discountPercent: Number(discountPercent),
        minOrderAmount: minOrderAmount ? Number(minOrderAmount) : null,
        maxUses: maxUses ? Number(maxUses) : null,
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : null,
      });
      setCode("");
      setDiscountPercent("");
      setMinOrderAmount("");
      setMaxUses("");
      setExpiresAt("");
      load();
    } catch (err) {
      setFormError(err instanceof ApiException ? err.message : "Erreur lors de la création.");
    } finally {
      setSubmitting(false);
    }
  }

  async function toggle(id: string, active: boolean) {
    setActingId(id);
    try {
      await apiClient.patch(`/api/promo-codes/${id}/${active ? "deactivate" : "activate"}`);
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  return (
    <div>
      <PageHeader title="Codes promo" subtitle="Codes plateforme — les codes boutique sont créés par les vendeurs" />

      <Card className="mb-6 max-w-3xl">
        <h2 className="mb-3 text-sm font-semibold text-foreground">Nouveau code plateforme</h2>
        {formError && (
          <div className="mb-3">
            <ErrorState message={formError} />
          </div>
        )}
        <form onSubmit={handleCreate} className="flex flex-wrap items-end gap-3">
          <div className="min-w-[140px]">
            <label className="mb-1 block text-xs font-medium text-muted">Code</label>
            <input
              required
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="MAPLENOU10"
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm uppercase outline-none focus:border-primary"
            />
          </div>
          <div className="w-28">
            <label className="mb-1 block text-xs font-medium text-muted">Réduction %</label>
            <input
              required
              type="number"
              min={1}
              max={100}
              value={discountPercent}
              onChange={(e) => setDiscountPercent(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            />
          </div>
          <div className="w-36">
            <label className="mb-1 block text-xs font-medium text-muted">Mini commande</label>
            <input
              type="number"
              min={0}
              value={minOrderAmount}
              onChange={(e) => setMinOrderAmount(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            />
          </div>
          <div className="w-28">
            <label className="mb-1 block text-xs font-medium text-muted">Utilisations max</label>
            <input
              type="number"
              min={1}
              value={maxUses}
              onChange={(e) => setMaxUses(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            />
          </div>
          <div className="min-w-[160px]">
            <label className="mb-1 block text-xs font-medium text-muted">Expire le</label>
            <input
              type="date"
              value={expiresAt}
              onChange={(e) => setExpiresAt(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            />
          </div>
          <Button type="submit" disabled={submitting}>
            Créer
          </Button>
        </form>
      </Card>

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <Card>
          {data.content.length === 0 ? (
            <EmptyState message="Aucun code promo plateforme." />
          ) : (
            <Table headers={["Code", "Réduction", "Mini commande", "Utilisations", "Expire le", "Statut", "Actions"]}>
              {data.content.map((p) => (
                <tr key={p.id}>
                  <td className="px-4 py-3 font-mono font-semibold text-foreground">{p.code}</td>
                  <td className="px-4 py-3 text-muted">{p.discountPercent}%</td>
                  <td className="px-4 py-3 text-muted">{formatFcfa(p.minOrderAmount)}</td>
                  <td className="px-4 py-3 text-muted">
                    {p.currentUses}
                    {p.maxUses ? ` / ${p.maxUses}` : ""}
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(p.expiresAt)}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={p.active ? "ACTIVE" : "INACTIVE"} />
                  </td>
                  <td className="px-4 py-3">
                    <Button variant="secondary" onClick={() => toggle(p.id, p.active)} disabled={actingId === p.id}>
                      {p.active ? "Désactiver" : "Activer"}
                    </Button>
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
