"use client";

import { useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { SubOrderResponse, SubOrderStatus } from "@/lib/types";
import { Button, Card, ErrorState, PageHeader, StatusBadge, formatDate, formatFcfa } from "@/components/ui";

const STATUSES: SubOrderStatus[] = [
  "PENDING",
  "PREPARING",
  "READY_FOR_PICKUP",
  "IN_DELIVERY",
  "DELIVERED",
  "CANCELLED",
  "RETURN_REQUESTED",
  "RETURNED",
];

export default function SubOrdersPage() {
  const [subOrderId, setSubOrderId] = useState("");
  const [status, setStatus] = useState<SubOrderStatus>("PENDING");
  const [result, setResult] = useState<SubOrderResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (
      !confirm(
        `Forcer le statut de la sous-commande ${subOrderId} à ${status} ?\n\nCeci contourne les transitions normales vendeur/livreur — à utiliser uniquement pour débloquer un cas de support.`,
      )
    )
      return;
    setError(null);
    setBusy(true);
    try {
      const res = await apiClient.patch<SubOrderResponse>(`/api/sub-orders/admin/${subOrderId}/status`, { status });
      setResult(res);
    } catch (e) {
      setError(e instanceof ApiException ? e.message : "Erreur.");
      setResult(null);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <PageHeader
        title="Forcer le statut d'une sous-commande"
        subtitle="Override admin — contourne les transitions normales vendeur/livreur, pour débloquer un cas de support. Pas de recherche possible : l'ID vient d'un ticket support, d'une commande ou d'une livraison."
      />

      <Card className="mb-6 max-w-xl">
        {error && (
          <div className="mb-3">
            <ErrorState message={error} />
          </div>
        )}
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <div>
            <label className="mb-1 block text-xs font-medium text-muted">ID de la sous-commande</label>
            <input
              required
              value={subOrderId}
              onChange={(e) => setSubOrderId(e.target.value)}
              placeholder="uuid"
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 font-mono text-sm outline-none focus:border-primary"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-muted">Nouveau statut</label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as SubOrderStatus)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            >
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </div>
          <Button type="submit" variant="danger" disabled={busy}>
            Forcer le statut
          </Button>
        </form>
      </Card>

      {result && (
        <Card className="max-w-xl">
          <h2 className="mb-3 text-sm font-semibold text-foreground">Résultat</h2>
          <dl className="grid grid-cols-[140px_1fr] gap-y-2 text-sm">
            <dt className="text-muted">Commande</dt>
            <dd className="text-foreground">#{result.orderNumber}</dd>
            <dt className="text-muted">Boutique</dt>
            <dd className="text-foreground">{result.shopName}</dd>
            <dt className="text-muted">Acheteur</dt>
            <dd className="text-foreground">{result.buyerName}</dd>
            <dt className="text-muted">Nouveau statut</dt>
            <dd>
              <StatusBadge status={result.status} />
            </dd>
            <dt className="text-muted">Sous-total</dt>
            <dd className="text-foreground">{formatFcfa(result.subtotal)}</dd>
            <dt className="text-muted">Mis à jour</dt>
            <dd className="text-foreground">{formatDate(result.updatedAt)}</dd>
          </dl>
        </Card>
      )}
    </div>
  );
}
