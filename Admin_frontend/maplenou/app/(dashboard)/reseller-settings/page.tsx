"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import { useAuth } from "@/lib/auth";
import { hasPermission } from "@/lib/permissions";
import type { Page, ResellerSettingsResponse } from "@/lib/types";
import { Button, Card, ErrorState, LoadingState, PageHeader, Table, formatDate } from "@/components/ui";

/** Champs numériques du formulaire ; les taux sont saisis en pourcentage et envoyés en fraction. */
interface Form {
  sharePct: string;
  minUnitPrice: string;
  tierThreshold: string;
  rateLowPct: string;
  rateHighPct: string;
  attributionDays: string;
  minWithdrawal: string;
  withdrawalFeePct: string;
  programActive: boolean;
  reason: string;
}

function toForm(s: ResellerSettingsResponse): Form {
  return {
    sharePct: String(+(s.shareRate * 100).toFixed(2)),
    minUnitPrice: String(s.minUnitPrice),
    tierThreshold: String(s.tierThreshold),
    rateLowPct: String(+(s.rateLow * 100).toFixed(2)),
    rateHighPct: String(+(s.rateHigh * 100).toFixed(2)),
    attributionDays: String(s.attributionDays),
    minWithdrawal: String(s.minWithdrawal),
    withdrawalFeePct: String(+(s.withdrawalFeeRate * 100).toFixed(2)),
    programActive: s.programActive,
    reason: "",
  };
}

const inputClass = "w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground";

export default function ResellerSettingsPage() {
  const { user } = useAuth();
  const canEdit = hasPermission(user, "RESELLERS_SETTINGS");
  const [current, setCurrent] = useState<ResellerSettingsResponse | null>(null);
  const [history, setHistory] = useState<ResellerSettingsResponse[]>([]);
  const [form, setForm] = useState<Form | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  function load() {
    Promise.all([
      apiClient.get<ResellerSettingsResponse>("/api/admin/resellers/settings"),
      apiClient.get<Page<ResellerSettingsResponse>>("/api/admin/resellers/settings/history", { page: 0, size: 20 }),
    ])
      .then(([c, h]) => {
        setCurrent(c);
        setForm(toForm(c));
        setHistory(h.content);
      })
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."));
  }

  useEffect(load, []);

  async function save() {
    if (!form) return;
    if (!form.reason.trim()) {
      alert("Un motif de modification est obligatoire.");
      return;
    }
    if (!confirm("Ces nouvelles règles ne s'appliqueront qu'aux NOUVELLES commandes. Confirmer ?")) return;
    setSaving(true);
    try {
      await apiClient.put("/api/admin/resellers/settings", {
        shareRate: Number(form.sharePct) / 100,
        minUnitPrice: Number(form.minUnitPrice),
        tierThreshold: Number(form.tierThreshold),
        rateLow: Number(form.rateLowPct) / 100,
        rateHigh: Number(form.rateHighPct) / 100,
        attributionDays: Number(form.attributionDays),
        minWithdrawal: Number(form.minWithdrawal),
        withdrawalFeeRate: Number(form.withdrawalFeePct) / 100,
        programActive: form.programActive,
        reason: form.reason.trim(),
      });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur lors de l'enregistrement.");
    } finally {
      setSaving(false);
    }
  }

  if (error) return <ErrorState message={error} />;
  if (!current || !form) return <LoadingState />;

  function field(label: string, key: keyof Form, hint?: string) {
    return (
      <label className="block text-sm">
        <span className="mb-1 block font-semibold text-foreground">{label}</span>
        <input
          className={inputClass}
          value={form![key] as string}
          disabled={!canEdit}
          onChange={(e) => setForm({ ...form!, [key]: e.target.value })}
        />
        {hint && <span className="mt-1 block text-xs text-muted">{hint}</span>}
      </label>
    );
  }

  return (
    <div>
      <PageHeader
        title="Paramètres du programme partenaire"
        subtitle={`Version en vigueur : v${current.version}. Chaque modification crée une nouvelle version ; les commandes déjà créées gardent leurs montants.`}
      />
      <Card>
        <div className="grid gap-4 p-4 md:grid-cols-2">
          {field("Part du partenaire (% de la commission Maplenou)", "sharePct")}
          {field("Prix unitaire minimum éligible (FCFA)", "minUnitPrice")}
          {field("Seuil des paliers (FCFA, inclus dans le palier bas)", "tierThreshold")}
          {field("Commission Maplenou, palier bas (%)", "rateLowPct", "De 2 500 FCFA jusqu'au seuil inclus")}
          {field("Commission Maplenou, palier haut (%)", "rateHighPct", "Au-dessus du seuil")}
          {field("Durée d'attribution après le dernier clic (jours)", "attributionDays")}
          {field("Retrait minimum (FCFA)", "minWithdrawal")}
          {field("Frais de retrait (%)", "withdrawalFeePct", "Prélevés uniquement au paiement effectif")}
          <label className="flex items-center gap-2 text-sm font-semibold text-foreground">
            <input
              type="checkbox"
              checked={form.programActive}
              disabled={!canEdit}
              onChange={(e) => setForm({ ...form, programActive: e.target.checked })}
            />
            Programme ouvert (nouvelles demandes, liens et attributions)
          </label>
          <div className="md:col-span-2">{field("Motif de la modification (obligatoire)", "reason")}</div>
        </div>
        {canEdit ? (
          <div className="px-4 pb-4">
            <Button onClick={save} disabled={saving}>
              Enregistrer une nouvelle version
            </Button>
          </div>
        ) : (
          <p className="px-4 pb-4 text-xs text-muted">Lecture seule : seul le Super Admin modifie ces paramètres.</p>
        )}
      </Card>

      <h2 className="mb-3 mt-8 text-base font-bold text-foreground">Historique des versions</h2>
      <Card>
        <Table headers={["Version", "Date", "Part", "Min.", "Seuil", "Taux bas/haut", "Attribution", "Retrait min.", "Frais", "Motif"]}>
          {history.map((h) => (
            <tr key={h.version}>
              <td className="px-4 py-3 font-semibold">v{h.version}</td>
              <td className="px-4 py-3 text-muted">{formatDate(h.createdAt)}</td>
              <td className="px-4 py-3">{+(h.shareRate * 100).toFixed(2)} %</td>
              <td className="px-4 py-3">{h.minUnitPrice}</td>
              <td className="px-4 py-3">{h.tierThreshold}</td>
              <td className="px-4 py-3">
                {+(h.rateLow * 100).toFixed(2)} % / {+(h.rateHigh * 100).toFixed(2)} %
              </td>
              <td className="px-4 py-3">{h.attributionDays} j</td>
              <td className="px-4 py-3">{h.minWithdrawal}</td>
              <td className="px-4 py-3">{+(h.withdrawalFeeRate * 100).toFixed(2)} %</td>
              <td className="px-4 py-3 text-muted">{h.changeReason}</td>
            </tr>
          ))}
        </Table>
      </Card>
    </div>
  );
}
