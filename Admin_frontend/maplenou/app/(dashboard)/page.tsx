"use client";

import { useEffect, useState } from "react";
import {
  Bar,
  BarChart,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { DashboardKpiResponse } from "@/lib/types";
import { Card, ErrorState, LoadingState, PageHeader, formatFcfa } from "@/components/ui";

const CHART_COLORS = {
  primary: "var(--color-primary)",
  accent: "var(--color-accent)",
  sage: "var(--color-sage)",
  error: "var(--color-error)",
  muted: "var(--color-muted)",
};

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

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
            <Card className="lg:col-span-1">
              <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-muted">
                Revenu vs commission
              </p>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart
                  data={[
                    {
                      name: "Période",
                      Revenu: data.revenue?.periodRevenue ?? 0,
                      Commission: data.revenue?.totalCommission ?? 0,
                    },
                  ]}
                >
                  <XAxis dataKey="name" stroke={CHART_COLORS.muted} fontSize={12} />
                  <YAxis stroke={CHART_COLORS.muted} fontSize={12} tickFormatter={(v) => `${v / 1000}k`} />
                  <Tooltip formatter={(v) => formatFcfa(typeof v === "number" ? v : Number(v))} />
                  <Legend />
                  <Bar dataKey="Revenu" fill={CHART_COLORS.primary} radius={[6, 6, 0, 0]} />
                  <Bar dataKey="Commission" fill={CHART_COLORS.accent} radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </Card>

            <Card className="lg:col-span-1">
              <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-muted">
                Répartition des boutiques
              </p>
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie
                    data={[
                      { name: "En attente", value: data.shops?.pending ?? 0, fill: CHART_COLORS.accent },
                      { name: "Approuvées", value: data.shops?.approved ?? 0, fill: CHART_COLORS.primary },
                      { name: "Suspendues", value: data.shops?.suspended ?? 0, fill: CHART_COLORS.error },
                    ]}
                    dataKey="value"
                    nameKey="name"
                    innerRadius={45}
                    outerRadius={75}
                    paddingAngle={2}
                  >
                    {[CHART_COLORS.accent, CHART_COLORS.primary, CHART_COLORS.error].map((color) => (
                      <Cell key={color} fill={color} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </Card>

            <Card className="lg:col-span-1">
              <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-muted">
                Statut des livraisons
              </p>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart
                  layout="vertical"
                  data={[
                    { name: "En attente", value: data.deliveries?.pending ?? 0 },
                    { name: "En cours", value: data.deliveries?.inTransit ?? 0 },
                    { name: "Livrées", value: data.deliveries?.delivered ?? 0 },
                    { name: "Échouées", value: data.deliveries?.failed ?? 0 },
                  ]}
                >
                  <XAxis type="number" stroke={CHART_COLORS.muted} fontSize={12} allowDecimals={false} />
                  <YAxis type="category" dataKey="name" stroke={CHART_COLORS.muted} fontSize={12} width={80} />
                  <Tooltip />
                  <Bar dataKey="value" fill={CHART_COLORS.sage} radius={[0, 6, 6, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </Card>
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
