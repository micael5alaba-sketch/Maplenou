"use client";

import { useEffect, useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { DeliveryZoneResponse } from "@/lib/types";
import { Button, Card, EmptyState, ErrorState, LoadingState, PageHeader, Table, formatFcfa } from "@/components/ui";

export default function DeliveryZonesPage() {
  const [zones, setZones] = useState<DeliveryZoneResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [fee, setFee] = useState("");
  const [pricePerKm, setPricePerKm] = useState("0");
  const [minutes, setMinutes] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<DeliveryZoneResponse[]>("/api/delivery-zones/admin")
      .then(setZones)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      await apiClient.post("/api/delivery-zones", {
        name,
        deliveryFee: Number(fee),
        pricePerKm: Number(pricePerKm || 0),
        estimatedTimeMinutes: Number(minutes),
      });
      setName("");
      setFee("");
      setPricePerKm("0");
      setMinutes("");
      load();
    } catch (err) {
      setFormError(err instanceof ApiException ? err.message : "Erreur lors de la création.");
    } finally {
      setSubmitting(false);
    }
  }

  async function toggle(id: string) {
    setActingId(id);
    try {
      await apiClient.patch(`/api/delivery-zones/${id}/toggle`);
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  async function editPricing(zone: DeliveryZoneResponse) {
    const newFee = prompt("Tarif de base (FCFA) :", String(zone.deliveryFee));
    if (newFee === null) return;
    const newPricePerKm = prompt(
      "Prix au km (FCFA, 0 = tarif fixe sans supplément distance) :",
      String(zone.pricePerKm),
    );
    if (newPricePerKm === null) return;

    setActingId(zone.id);
    try {
      await apiClient.patch(`/api/delivery-zones/${zone.id}`, {
        deliveryFee: Number(newFee),
        pricePerKm: Number(newPricePerKm),
      });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  async function remove(id: string) {
    if (!confirm("Supprimer cette zone ?")) return;
    setActingId(id);
    try {
      await apiClient.delete(`/api/delivery-zones/${id}`);
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  return (
    <div>
      <PageHeader
        title="Zones de livraison"
        subtitle="Tarif de base + prix au km par zone — dynamique type Gozem, calculé sur la distance réelle boutique↔adresse"
      />

      <Card className="mb-6 max-w-3xl">
        <h2 className="mb-3 text-sm font-semibold text-foreground">Nouvelle zone</h2>
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
              placeholder="Ex: Adidogomé"
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            />
          </div>
          <div className="w-32">
            <label className="mb-1 block text-xs font-medium text-muted">Tarif de base</label>
            <input
              required
              type="number"
              min={0}
              value={fee}
              onChange={(e) => setFee(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            />
          </div>
          <div className="w-32">
            <label className="mb-1 block text-xs font-medium text-muted">Prix/km</label>
            <input
              type="number"
              min={0}
              value={pricePerKm}
              onChange={(e) => setPricePerKm(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            />
          </div>
          <div className="w-32">
            <label className="mb-1 block text-xs font-medium text-muted">Délai (min)</label>
            <input
              required
              type="number"
              min={1}
              value={minutes}
              onChange={(e) => setMinutes(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            />
          </div>
          <Button type="submit" disabled={submitting}>
            Ajouter
          </Button>
        </form>
        <p className="mt-3 text-xs text-muted">
          Prix/km à 0 = tarif fixe (comportement historique). Au-dessus de 0, le prix affiché et facturé à
          l&apos;acheteur = tarif de base + distance réelle × prix/km.
        </p>
      </Card>

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {zones && (
        <Card>
          {zones.length === 0 ? (
            <EmptyState message="Aucune zone." />
          ) : (
            <Table headers={["Zone", "Tarif de base", "Prix/km", "Délai estimé", "Statut", "Actions"]}>
              {zones.map((z) => (
                <tr key={z.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{z.name}</td>
                  <td className="px-4 py-3 text-muted">{formatFcfa(z.deliveryFee)}</td>
                  <td className="px-4 py-3 text-muted">
                    {z.pricePerKm > 0 ? `${formatFcfa(z.pricePerKm)}/km` : "—"}
                  </td>
                  <td className="px-4 py-3 text-muted">{z.estimatedTimeMinutes} min</td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                        z.active ? "bg-primary/10 text-primary" : "bg-muted/10 text-muted"
                      }`}
                    >
                      {z.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <Button variant="secondary" onClick={() => editPricing(z)} disabled={actingId === z.id}>
                        Modifier le tarif
                      </Button>
                      <Button variant="secondary" onClick={() => toggle(z.id)} disabled={actingId === z.id}>
                        {z.active ? "Désactiver" : "Activer"}
                      </Button>
                      <Button variant="danger" onClick={() => remove(z.id)} disabled={actingId === z.id}>
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
