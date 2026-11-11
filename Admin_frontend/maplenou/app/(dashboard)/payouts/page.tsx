"use client";

import { useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import { useAuth } from "@/lib/auth";
import { hasPermission } from "@/lib/permissions";
import type { PayoutResponse } from "@/lib/types";
import { Button, Card, ErrorState, PageHeader, formatDate, formatFcfa } from "@/components/ui";
import { WithdrawalsManager } from "@/components/WithdrawalsManager";

export default function PayoutsPage() {
  const { user } = useAuth();
  const canGenerate = hasPermission(user, "PAYOUTS_PREPARE");

  const [periodStart, setPeriodStart] = useState("");
  const [periodEnd, setPeriodEnd] = useState("");
  const [genError, setGenError] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  async function handleGenerate(e: FormEvent) {
    e.preventDefault();
    setGenError(null);
    setGenerating(true);
    try {
      const res = await apiClient.post<{ payoutsCreated: number }>("/api/admin/payouts/generate", {
        periodStart: new Date(periodStart).toISOString(),
        periodEnd: new Date(periodEnd).toISOString(),
      });
      alert(`${res.payoutsCreated} reversement(s) généré(s), à vérifier.`);
      setReloadKey((k) => k + 1);
    } catch (err) {
      setGenError(err instanceof ApiException ? err.message : "Erreur lors de la génération.");
    } finally {
      setGenerating(false);
    }
  }

  return (
    <div>
      <PageHeader
        title="Retraits vendeurs"
        subtitle="Circuit obligatoire : réception, vérification et soumission par l'Agent support, décision du Super Admin avec MFA, paiement."
      />

      {canGenerate && (
        <Card className="mb-6 max-w-xl">
          <h2 className="mb-3 text-sm font-semibold text-foreground">Génération manuelle (test / rattrapage)</h2>
          {genError && (
            <div className="mb-3">
              <ErrorState message={genError} />
            </div>
          )}
          <form onSubmit={handleGenerate} className="flex flex-wrap items-end gap-3">
            <div className="min-w-[160px] flex-1">
              <label className="mb-1 block text-xs font-medium text-muted">Début période</label>
              <input
                required
                type="date"
                value={periodStart}
                onChange={(e) => setPeriodStart(e.target.value)}
                className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
              />
            </div>
            <div className="min-w-[160px] flex-1">
              <label className="mb-1 block text-xs font-medium text-muted">Fin période</label>
              <input
                required
                type="date"
                value={periodEnd}
                onChange={(e) => setPeriodEnd(e.target.value)}
                className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
              />
            </div>
            <Button type="submit" disabled={generating}>
              Générer
            </Button>
          </form>
        </Card>
      )}

      <WithdrawalsManager<PayoutResponse>
        key={reloadKey}
        basePath="/api/admin/payouts"
        title="retrait vendeur"
        columns={["Boutique", "Période", "Montant net", "Commission", "Frais"]}
        renderCells={(p) => (
          <>
            <td className="px-4 py-3 font-medium text-foreground">{p.shopName}</td>
            <td className="px-4 py-3 text-muted">
              {formatDate(p.periodStart)} → {formatDate(p.periodEnd)}
            </td>
            <td className="px-4 py-3 font-semibold text-foreground">{formatFcfa(p.amount)}</td>
            <td className="px-4 py-3 text-muted">{formatFcfa(p.commissionTotal)}</td>
            <td className="px-4 py-3 text-muted">{formatFcfa(p.feesDeducted)}</td>
          </>
        )}
      />
    </div>
  );
}
