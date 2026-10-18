"use client";

import { useEffect, useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { DeliveryPricingTierResponse } from "@/lib/types";
import { Button, Card, EmptyState, ErrorState, LoadingState, PageHeader, Table, formatFcfa } from "@/components/ui";

export default function DeliveryPricingPage() {
  const [tiers, setTiers] = useState<DeliveryPricingTierResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  const [maxDistanceKm, setMaxDistanceKm] = useState("");
  const [unlimited, setUnlimited] = useState(false);
  const [price, setPrice] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<DeliveryPricingTierResponse[]>("/api/delivery-pricing-tiers")
      .then(setTiers)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      await apiClient.post("/api/delivery-pricing-tiers", {
        maxDistanceKm: unlimited ? null : Number(maxDistanceKm),
        price: Number(price),
      });
      setMaxDistanceKm("");
      setUnlimited(false);
      setPrice("");
      load();
    } catch (err) {
      setFormError(err instanceof ApiException ? err.message : "Erreur lors de la création.");
    } finally {
      setSubmitting(false);
    }
  }

  async function editPrice(tier: DeliveryPricingTierResponse) {
    const newPrice = prompt("Nouveau prix (FCFA) :", String(tier.price));
    if (newPrice === null) return;
    let newMax: number | null = tier.maxDistanceKm;
    if (tier.maxDistanceKm !== null) {
      const input = prompt("Distance maximale (km) :", String(tier.maxDistanceKm));
      if (input === null) return;
      newMax = Number(input);
    }

    setActingId(tier.id);
    try {
      await apiClient.put(`/api/delivery-pricing-tiers/${tier.id}`, {
        maxDistanceKm: newMax,
        price: Number(newPrice),
      });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  async function remove(id: string) {
    if (!confirm("Supprimer ce palier ?")) return;
    setActingId(id);
    try {
      await apiClient.delete(`/api/delivery-pricing-tiers/${id}`);
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  const sorted = tiers
    ? [...tiers].sort((a, b) => (a.maxDistanceKm ?? Infinity) - (b.maxDistanceKm ?? Infinity))
    : null;

  return (
    <div>
      <PageHeader
        title="Tarification livraison"
        subtitle="Barème par distance réelle (boutique ↔ acheteur, calculée automatiquement par GPS) — plus de zones à gérer : juste un prix par tranche de kilomètres."
      />

      <Card className="mb-6 max-w-2xl">
        <h2 className="mb-3 text-sm font-semibold text-foreground">Nouveau palier</h2>
        {formError && (
          <div className="mb-3">
            <ErrorState message={formError} />
          </div>
        )}
        <form onSubmit={handleCreate} className="flex flex-wrap items-end gap-3">
          <div className="w-40">
            <label className="mb-1 block text-xs font-medium text-muted">Jusqu&apos;à (km)</label>
            <input
              required={!unlimited}
              disabled={unlimited}
              type="number"
              min={0.01}
              step={0.1}
              value={maxDistanceKm}
              onChange={(e) => setMaxDistanceKm(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary disabled:opacity-50"
            />
          </div>
          <label className="flex items-center gap-2 pb-2 text-sm text-foreground">
            <input type="checkbox" checked={unlimited} onChange={(e) => setUnlimited(e.target.checked)} />
            Au-delà (illimité)
          </label>
          <div className="w-40">
            <label className="mb-1 block text-xs font-medium text-muted">Prix (FCFA)</label>
            <input
              required
              type="number"
              min={0}
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            />
          </div>
          <Button type="submit" disabled={submitting}>
            Ajouter
          </Button>
        </form>
        <p className="mt-3 text-xs text-muted">
          Ex : jusqu&apos;à 5 km → 500 FCFA, jusqu&apos;à 10 km → 1 000 FCFA, au-delà → 1 500 FCFA. Un seul
          palier &laquo;&nbsp;au-delà&nbsp;&raquo; autorisé — c&apos;est lui qui s&apos;applique à toute
          distance non couverte par un palier plus bas.
        </p>
      </Card>

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {sorted && (
        <Card>
          {sorted.length === 0 ? (
            <EmptyState message="Aucun palier configuré — aucune livraison ne pourra être tarifée tant qu'il n'y en a pas au moins un." />
          ) : (
            <Table headers={["Distance", "Prix", "Actions"]}>
              {sorted.map((t) => (
                <tr key={t.id}>
                  <td className="px-4 py-3 font-medium text-foreground">
                    {t.maxDistanceKm !== null ? `Jusqu'à ${t.maxDistanceKm} km` : "Au-delà"}
                  </td>
                  <td className="px-4 py-3 text-muted">{formatFcfa(t.price)}</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <Button variant="secondary" onClick={() => editPrice(t)} disabled={actingId === t.id}>
                        Modifier
                      </Button>
                      <Button variant="danger" onClick={() => remove(t.id)} disabled={actingId === t.id}>
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
