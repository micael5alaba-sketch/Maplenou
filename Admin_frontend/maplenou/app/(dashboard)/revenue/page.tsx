"use client";

import { useEffect, useState } from "react";
import { Bar, BarChart, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { Page, RevenueEntry, RevenueSource, RevenueSummary } from "@/lib/types";
import {
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Pagination,
  Table,
  formatDate,
  formatFcfa,
} from "@/components/ui";

const SOURCES: { value: RevenueSource; label: string; reference: string; detail: string }[] = [
  { value: "COMMISSION", label: "Commissions", reference: "Commande", detail: "Boutique" },
  { value: "DELIVERY_SHARE", label: "Part livraison", reference: "Commande", detail: "Livreur" },
  { value: "SELLER_FEE", label: "Frais mensuels vendeurs", reference: "Mois facturé", detail: "Boutique" },
];

function isoDay(d: Date): string {
  return d.toISOString().slice(0, 10);
}

/**
 * Registre des revenus de la plateforme (Super Admin et Auditeur) : ce que Maplenou garde, source
 * par source, ligne par ligne. Calculé depuis les commandes, livraisons et retraits déjà enregistrés
 * (aucune saisie manuelle). La commission est reconnue à la livraison de la sous-commande.
 */
export default function RevenuePage() {
  const today = new Date();
  const [from, setFrom] = useState(isoDay(new Date(today.getTime() - 29 * 86400000)));
  const [to, setTo] = useState(isoDay(today));
  const [summary, setSummary] = useState<RevenueSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const [source, setSource] = useState<RevenueSource>("COMMISSION");
  const [page, setPage] = useState(0);
  const [entries, setEntries] = useState<Page<RevenueEntry> | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    apiClient
      .get<RevenueSummary>("/api/admin/revenue", { from, to })
      .then(setSummary)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }, [from, to]);

  useEffect(() => {
    apiClient
      .get<Page<RevenueEntry>>("/api/admin/revenue/entries", { source, from, to, page, size: 20 })
      .then(setEntries)
      .catch(() => setEntries(null));
  }, [source, from, to, page]);

  async function exportCsv() {
    try {
      const all = await apiClient.get<Page<RevenueEntry>>("/api/admin/revenue/entries", { source, from, to, page: 0, size: 1000 });
      const header = "date;source;reference;detail;montant_fcfa";
      const lines = all.content.map((e) =>
        [e.date, e.source, e.reference, e.label.replaceAll(";", ","), e.amount].join(";"),
      );
      const blob = new Blob([[header, ...lines].join("\n")], { type: "text/csv;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `revenus-${source.toLowerCase()}-${from}-${to}.csv`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Export impossible.");
    }
  }

  const input = "rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary";
  const current = SOURCES.find((s) => s.value === source)!;

  return (
    <div>
      <PageHeader
        title="Revenus de la plateforme"
        subtitle="Ce que Maplenou garde, source par source. Commission reconnue à la livraison ; annulations et retours exclus."
      />

      <div className="mb-6 flex flex-wrap items-end gap-3">
        <div>
          <label className="mb-1 block text-xs font-medium text-muted">Du</label>
          <input type="date" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0); }} className={input} />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-muted">Au</label>
          <input type="date" value={to} onChange={(e) => { setTo(e.target.value); setPage(0); }} className={input} />
        </div>
      </div>

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {summary && (
        <>
          <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
            <Card>
              <p className="text-xs font-semibold uppercase tracking-wide text-muted">Revenus nets</p>
              <p className="mt-1 text-2xl font-bold text-primary">{formatFcfa(summary.total)}</p>
            </Card>
            <Card>
              <p className="text-xs font-semibold uppercase tracking-wide text-muted">Commissions</p>
              <p className="mt-1 text-2xl font-bold text-foreground">{formatFcfa(summary.commissions)}</p>
            </Card>
            <Card>
              <p className="text-xs font-semibold uppercase tracking-wide text-muted">Part livraison</p>
              <p className="mt-1 text-2xl font-bold text-foreground">{formatFcfa(summary.deliveryShare)}</p>
            </Card>
            <Card>
              <p className="text-xs font-semibold uppercase tracking-wide text-muted">Frais mensuels vendeurs</p>
              <p className="mt-1 text-2xl font-bold text-foreground">{formatFcfa(summary.sellerFees)}</p>
            </Card>
            <Card>
              <p className="text-xs font-semibold uppercase tracking-wide text-muted">Parts partenaires (déduites)</p>
              <p className="mt-1 text-2xl font-bold text-foreground">{formatFcfa(summary.partnerShares ?? 0)}</p>
            </Card>
          </div>

          <Card className="mb-6">
            <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-muted">Revenus par jour</p>
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={summary.daily.map((d) => ({ jour: d.date.slice(5), Commissions: d.commissions, Livraison: d.deliveryShare, "Frais vendeurs": d.sellerFees }))}>
                <XAxis dataKey="jour" fontSize={11} />
                <YAxis fontSize={11} tickFormatter={(v) => `${v / 1000}k`} />
                <Tooltip formatter={(v) => formatFcfa(typeof v === "number" ? v : Number(v))} />
                <Legend />
                <Bar dataKey="Commissions" stackId="r" fill="var(--color-primary)" />
                <Bar dataKey="Livraison" stackId="r" fill="var(--color-accent)" />
                <Bar dataKey="Frais vendeurs" stackId="r" fill="var(--color-sage)" />
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </>
      )}

      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap gap-2">
          {SOURCES.map((s) => (
            <button
              key={s.value}
              onClick={() => { setSource(s.value); setPage(0); }}
              className={`rounded-full px-4 py-1.5 text-sm font-semibold transition ${source === s.value ? "bg-primary text-white" : "bg-input-fill text-foreground"}`}
            >
              {s.label}
            </button>
          ))}
        </div>
        <button onClick={exportCsv} className="rounded-full border border-border px-4 py-1.5 text-sm font-semibold text-foreground hover:bg-input-fill">
          Exporter en CSV
        </button>
      </div>

      {entries && (
        <Card>
          {entries.content.length === 0 ? (
            <EmptyState message="Aucune ligne sur cette période." />
          ) : (
            <Table headers={["Date", current.reference, current.detail, "Montant"]}>
              {entries.content.map((e) => (
                <tr key={`${e.source}-${e.referenceId}`}>
                  <td className="px-4 py-3 text-muted">{formatDate(e.date)}</td>
                  <td className="px-4 py-3 font-medium text-foreground">{e.reference}</td>
                  <td className="px-4 py-3 text-muted">{e.label}</td>
                  <td className="px-4 py-3 font-semibold text-foreground">{formatFcfa(e.amount)}</td>
                </tr>
              ))}
            </Table>
          )}
        </Card>
      )}
      {entries && <Pagination page={entries.number} totalPages={entries.totalPages} onChange={setPage} />}
    </div>
  );
}
