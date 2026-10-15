"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { DashboardKpiResponse } from "@/lib/types";
import { Card, ErrorState, LoadingState, PageHeader, formatFcfa } from "@/components/ui";

const PERIODS = [
  { value: "TODAY", label: "Aujourd'hui" },
  { value: "LAST_7_DAYS", label: "7 derniers jours" },
  { value: "LAST_30_DAYS", label: "30 derniers jours" },
  { value: "ALL_TIME", label: "Depuis le début" },
];

export default function DashboardHomePage() {
  const [period, setPeriod] = useState("LAST_30_DAYS");
  const [data, setData] = useState<DashboardKpiResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    setError(null);
    apiClient
      .get<DashboardKpiResponse>("/api/admin/kpis", { period })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }, [period]);

  return (
    <div>
      <PageHeader
        title="Tableau de bord"
        subtitle="Vue d'ensemble de l'activité Maplenou"
        action={
          <select
            value={period}
            onChange={(e) => setPeriod(e.target.value)}
            className="rounded-xl border border-border bg-surface px-3 py-2 text-sm"
          >
            {PERIODS.map((p) => (
              <option key={p.value} value={p.value}>
                {p.label}
              </option>
            ))}
          </select>
        }
      />

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <div className="space-y-6">
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <KpiCard label="Revenu de la période" value={formatFcfa(data.revenue?.periodRevenue)} />
            <KpiCard label="Commission totale" value={formatFcfa(data.revenue?.totalCommission)} />
            <KpiCard label="Nouvelles commandes" value={String(data.orders?.periodNew ?? 0)} />
            <KpiCard label="Nouveaux utilisateurs" value={String(data.users?.periodNew ?? 0)} />
          </div>

          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <KpiCard label="Boutiques en attente" value={String(data.shops?.pending ?? 0)} accent />
            <KpiCard label="Boutiques approuvées" value={String(data.shops?.approved ?? 0)} />
            <KpiCard label="Livraisons en attente" value={String(data.deliveries?.pending ?? 0)} accent />
            <KpiCard label="Livraisons livrées" value={String(data.deliveries?.delivered ?? 0)} />
          </div>
        </div>
      )}
    </div>
  );
}

function KpiCard({ label, value, accent }: { label: string; value: string; accent?: boolean }) {
  return (
    <Card>
      <p className="text-xs font-semibold uppercase tracking-wide text-muted">{label}</p>
      <p className={`mt-2 text-2xl font-bold ${accent ? "text-accent" : "text-foreground"}`}>{value}</p>
    </Card>
  );
}
