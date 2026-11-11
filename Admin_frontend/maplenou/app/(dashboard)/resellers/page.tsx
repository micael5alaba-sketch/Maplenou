"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import { useAuth } from "@/lib/auth";
import { hasPermission } from "@/lib/permissions";
import type { Page, ResellerProfileResponse, ResellerStatus, ResellerWalletResponse } from "@/lib/types";
import {
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Pagination,
  StatusBadge,
  Table,
  formatDate,
  formatFcfa,
} from "@/components/ui";

const STATUS_FILTERS: { value: ResellerStatus | null; label: string }[] = [
  { value: "PENDING", label: "En attente" },
  { value: "APPROVED", label: "Actifs" },
  { value: "SUSPENDED", label: "Suspendus" },
  { value: "REJECTED", label: "Refusés" },
  { value: "DEACTIVATED", label: "Désactivés" },
  { value: null, label: "Tous" },
];

const STATUS_LABELS: Record<string, string> = {
  PENDING: "En attente",
  APPROVED: "Actif",
  SUSPENDED: "Suspendu",
  REJECTED: "Refusé",
  DEACTIVATED: "Désactivé",
};

export default function ResellersPage() {
  const { user } = useAuth();
  const canWrite = hasPermission(user, "RESELLERS_WRITE");
  const canAdjust = hasPermission(user, "RESELLERS_SETTINGS");

  const [status, setStatus] = useState<ResellerStatus | null>("PENDING");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<ResellerProfileResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);
  const [walletFor, setWalletFor] = useState<string | null>(null);
  const [wallet, setWallet] = useState<ResellerWalletResponse | null>(null);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<ResellerProfileResponse>>("/api/admin/resellers", { status: status ?? undefined, page, size: 20 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [status, page]);

  async function act(id: string, call: () => Promise<unknown>) {
    setActingId(id);
    try {
      await call();
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  function withReason(id: string, action: string, question: string) {
    const reason = prompt(question);
    if (reason === null) return;
    if (!reason.trim()) {
      alert("Un motif est obligatoire.");
      return;
    }
    return act(id, () => apiClient.patch(`/api/admin/resellers/${id}/${action}`, { reason: reason.trim() }));
  }

  async function showWallet(id: string) {
    if (walletFor === id) {
      setWalletFor(null);
      return;
    }
    try {
      setWallet(await apiClient.get<ResellerWalletResponse>(`/api/admin/resellers/${id}/wallet`));
      setWalletFor(id);
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    }
  }

  function adjust(id: string) {
    const amount = Number(prompt("Montant de la régularisation en FCFA (négatif pour retirer) :"));
    if (!Number.isInteger(amount) || amount === 0) return;
    const reason = prompt("Motif de la régularisation (obligatoire) :");
    if (!reason?.trim()) return;
    return act(id, () =>
      apiClient.post(`/api/admin/resellers/${id}/adjustments`, {
        amount,
        reason: reason.trim(),
        requestKey: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      }),
    );
  }

  return (
    <div>
      <PageHeader
        title="Partenaires revendeurs"
        subtitle="Demandes d'adhésion et gestion des partenaires. Toute décision est tracée avec son auteur et son motif."
      />
      <div className="mb-4 flex flex-wrap gap-2">
        {STATUS_FILTERS.map((f) => (
          <button
            key={f.label}
            onClick={() => {
              setStatus(f.value);
              setPage(0);
            }}
            className={`rounded-full px-4 py-1.5 text-sm font-semibold transition ${
              status === f.value ? "bg-primary text-white" : "bg-input-fill text-foreground"
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
            <EmptyState message="Aucun partenaire dans cette catégorie." />
          ) : (
            <Table headers={["Partenaire", "Contact", "Conditions acceptées", "Statut", "Actions"]}>
              {data.content.flatMap((p) => {
                const busy = actingId === p.id;
                return [
                  <tr key={p.id}>
                    <td className="px-4 py-3 font-medium text-foreground">
                      {p.fullName}
                      <span className="block text-xs text-muted">Demande du {formatDate(p.createdAt)}</span>
                    </td>
                    <td className="px-4 py-3 text-muted">
                      {p.phoneNumber}
                      <span className="block text-xs">{p.email}</span>
                    </td>
                    <td className="px-4 py-3 text-muted">
                      v{p.termsVersion} — {formatDate(p.termsAcceptedAt)}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={p.status} label={STATUS_LABELS[p.status]} />
                      {p.statusReason && <span className="block text-xs text-muted">{p.statusReason}</span>}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-2">
                        {canWrite && p.status === "PENDING" && (
                          <>
                            <Button disabled={busy} onClick={() => act(p.id, () => apiClient.patch(`/api/admin/resellers/${p.id}/approve`, {}))}>
                              Approuver
                            </Button>
                            <Button variant="danger" disabled={busy} onClick={() => withReason(p.id, "reject", "Motif du refus (obligatoire) :")}>
                              Refuser
                            </Button>
                          </>
                        )}
                        {canWrite && p.status === "APPROVED" && (
                          <>
                            <Button variant="secondary" disabled={busy} onClick={() => withReason(p.id, "suspend", "Motif de la suspension :")}>
                              Suspendre
                            </Button>
                            <Button variant="danger" disabled={busy} onClick={() => withReason(p.id, "deactivate", "Motif de la désactivation :")}>
                              Désactiver
                            </Button>
                          </>
                        )}
                        {canWrite && (p.status === "SUSPENDED" || p.status === "DEACTIVATED") && (
                          <Button disabled={busy} onClick={() => withReason(p.id, "reactivate", "Motif de la réactivation :")}>
                            Réactiver
                          </Button>
                        )}
                        {p.status !== "PENDING" && p.status !== "REJECTED" && (
                          <Button variant="secondary" onClick={() => showWallet(p.id)}>
                            {walletFor === p.id ? "Masquer le portefeuille" : "Portefeuille"}
                          </Button>
                        )}
                        {canAdjust && p.status !== "PENDING" && p.status !== "REJECTED" && (
                          <Button variant="secondary" disabled={busy} onClick={() => adjust(p.id)}>
                            Régulariser
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>,
                  walletFor === p.id && wallet ? (
                    <tr key={`${p.id}-wallet`}>
                      <td colSpan={5} className="bg-input-fill px-4 py-3 text-xs text-muted">
                        Disponible <b className="text-foreground">{formatFcfa(wallet.available)}</b> · Réservé (retrait en cours){" "}
                        <b className="text-foreground">{formatFcfa(wallet.reserved)}</b> · En attente de livraison{" "}
                        <b className="text-foreground">{formatFcfa(wallet.pending)}</b> · Total gagné{" "}
                        <b className="text-foreground">{formatFcfa(wallet.totalEarned)}</b> · Total retiré{" "}
                        <b className="text-foreground">{formatFcfa(wallet.totalPaid)}</b> · Frais payés{" "}
                        <b className="text-foreground">{formatFcfa(wallet.totalFees)}</b> · Clics comptés{" "}
                        <b className="text-foreground">{wallet.clicks}</b>
                      </td>
                    </tr>
                  ) : null,
                ];
              })}
            </Table>
          )}
        </Card>
      )}
      {data && <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  );
}
