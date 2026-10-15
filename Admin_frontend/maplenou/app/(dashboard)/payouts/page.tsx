"use client";

import { useEffect, useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { Page, PayoutResponse, PayoutStatus } from "@/lib/types";
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

const FILTERS: { label: string; value: PayoutStatus | "ALL" }[] = [
  { label: "En attente", value: "PENDING" },
  { label: "En traitement", value: "PROCESSING" },
  { label: "Payés", value: "PAID" },
  { label: "Échoués", value: "FAILED" },
  { label: "Tous", value: "ALL" },
];

export default function PayoutsPage() {
  const [filter, setFilter] = useState<PayoutStatus | "ALL">("PENDING");
  const [data, setData] = useState<Page<PayoutResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  const [periodStart, setPeriodStart] = useState("");
  const [periodEnd, setPeriodEnd] = useState("");
  const [genError, setGenError] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<PayoutResponse>>("/api/admin/payouts", {
        status: filter === "ALL" ? undefined : filter,
        size: 50,
      })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [filter]);

  async function handleGenerate(e: FormEvent) {
    e.preventDefault();
    setGenError(null);
    setGenerating(true);
    try {
      const res = await apiClient.post<{ payoutsCreated: number }>("/api/admin/payouts/generate", {
        periodStart: new Date(periodStart).toISOString(),
        periodEnd: new Date(periodEnd).toISOString(),
      });
      alert(`${res.payoutsCreated} reversement(s) généré(s).`);
      load();
    } catch (err) {
      setGenError(err instanceof ApiException ? err.message : "Erreur lors de la génération.");
    } finally {
      setGenerating(false);
    }
  }

  async function markProcessing(id: string) {
    setActingId(id);
    try {
      await apiClient.patch(`/api/admin/payouts/${id}/processing`);
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  async function markPaid(id: string) {
    const notes = prompt("Référence du virement (optionnel) :") ?? "";
    setActingId(id);
    try {
      await apiClient.patch(`/api/admin/payouts/${id}/paid`, { status: "PAID", notes });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  async function markFailed(id: string) {
    const notes = prompt("Raison de l'échec :") ?? "";
    setActingId(id);
    try {
      await apiClient.patch(`/api/admin/payouts/${id}/failed`, { status: "FAILED", notes });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  return (
    <div>
      <PageHeader title="Reversements vendeurs" subtitle="Hebdomadaire, déclenché après SubOrder.DELIVERED" />

      <Card className="mb-6 max-w-xl">
        <h2 className="mb-3 text-sm font-semibold text-foreground">Génération manuelle (test / rattrapage)</h2>
        {genError && (
          <div className="mb-3">
            <ErrorState message={genError} />
          </div>
        )}
        <form onSubmit={handleGenerate} className="flex flex-wrap items-end gap-3">
          <div className="flex-1 min-w-[160px]">
            <label className="mb-1 block text-xs font-medium text-muted">Début période</label>
            <input
              required
              type="date"
              value={periodStart}
              onChange={(e) => setPeriodStart(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
            />
          </div>
          <div className="flex-1 min-w-[160px]">
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

      <div className="mb-4 flex gap-2">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            onClick={() => setFilter(f.value)}
            className={`rounded-full px-4 py-1.5 text-sm font-semibold transition ${
              filter === f.value ? "bg-primary text-white" : "bg-input-fill text-foreground"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <Card>
          {data.content.length === 0 ? (
            <EmptyState message="Aucun reversement." />
          ) : (
            <Table headers={["Boutique", "Période", "Montant net", "Commission", "Frais", "Statut", "Actions"]}>
              {data.content.map((p) => (
                <tr key={p.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{p.shopName}</td>
                  <td className="px-4 py-3 text-muted">
                    {formatDate(p.periodStart)} → {formatDate(p.periodEnd)}
                  </td>
                  <td className="px-4 py-3 font-semibold text-foreground">{formatFcfa(p.amount)}</td>
                  <td className="px-4 py-3 text-muted">{formatFcfa(p.commissionTotal)}</td>
                  <td className="px-4 py-3 text-muted">{formatFcfa(p.feesDeducted)}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={p.status} />
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      {p.status === "PENDING" && (
                        <Button variant="secondary" onClick={() => markProcessing(p.id)} disabled={actingId === p.id}>
                          En traitement
                        </Button>
                      )}
                      {(p.status === "PENDING" || p.status === "PROCESSING") && (
                        <>
                          <Button onClick={() => markPaid(p.id)} disabled={actingId === p.id}>
                            Marquer payé
                          </Button>
                          <Button variant="danger" onClick={() => markFailed(p.id)} disabled={actingId === p.id}>
                            Échoué
                          </Button>
                        </>
                      )}
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
