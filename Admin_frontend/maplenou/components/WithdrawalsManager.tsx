"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import { useAuth } from "@/lib/auth";
import { hasPermission } from "@/lib/permissions";
import type { Page, PayoutStatus, WithdrawalHistoryEntry } from "@/lib/types";
import {
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  Pagination,
  StatusBadge,
  Table,
  formatDate,
  formatFcfa,
} from "@/components/ui";

/** Champs communs aux retraits vendeurs et livreurs (le circuit est le même). */
export interface WithdrawalRow {
  id: string;
  amount: number;
  status: PayoutStatus;
  notes: string | null;
  createdAt: string;
  approvedBy: string | null;
  approvedAt: string | null;
}

export const STATUS_LABELS: Record<PayoutStatus, string> = {
  PENDING: "Reçu",
  PREPARED: "Préparé",
  SUBMITTED: "Soumis au Super Admin",
  NEEDS_CORRECTION: "À corriger",
  APPROVED: "Approuvé",
  REJECTED: "Rejeté",
  PROCESSING: "Virement en cours",
  PAID: "Payé",
  FAILED: "Échec du virement",
};

const FILTERS: { label: string; value: PayoutStatus | "ALL" }[] = [
  { label: "Reçus", value: "PENDING" },
  { label: "À corriger", value: "NEEDS_CORRECTION" },
  { label: "Préparés", value: "PREPARED" },
  { label: "À approuver", value: "SUBMITTED" },
  { label: "Approuvés", value: "APPROVED" },
  { label: "En cours", value: "PROCESSING" },
  { label: "Payés", value: "PAID" },
  { label: "Rejetés", value: "REJECTED" },
  { label: "Échecs", value: "FAILED" },
  { label: "Tous", value: "ALL" },
];

/**
 * Circuit des retraits (document "Rôles et accès", §8) : l'Agent support reçoit, vérifie et soumet ;
 * seul le Super Admin approuve (code MFA redemandé), rejette (justification) et marque payé.
 * Les boutons suivent les permissions, mais le serveur refuse de toute façon une action non autorisée.
 */
export function WithdrawalsManager<T extends WithdrawalRow>({
  basePath,
  columns,
  renderCells,
  title,
}: {
  /** Ex. "/api/admin/payouts" ou "/api/admin/courier-withdrawals". */
  basePath: string;
  columns: string[];
  renderCells: (row: T) => React.ReactNode;
  title: string;
}) {
  const { user } = useAuth();
  const canPrepare = hasPermission(user, "PAYOUTS_PREPARE");
  const canApprove = hasPermission(user, "PAYOUTS_APPROVE");

  const [filter, setFilter] = useState<PayoutStatus | "ALL">("PENDING");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<T> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);
  const [historyFor, setHistoryFor] = useState<string | null>(null);
  const [history, setHistory] = useState<WithdrawalHistoryEntry[]>([]);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<T>>(basePath, { status: filter === "ALL" ? undefined : filter, page, size: 20 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [filter, page, basePath]);

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

  function askJustification(message: string): string | null {
    const value = prompt(message);
    if (value === null) return null;
    if (!value.trim()) {
      alert("Une justification est obligatoire.");
      return null;
    }
    return value.trim();
  }

  const prepare = (id: string) => act(id, () => apiClient.patch(`${basePath}/${id}/prepare`, {}));
  const submit = (id: string) => act(id, () => apiClient.patch(`${basePath}/${id}/submit`, {}));
  const approve = (id: string) => {
    // Nouvelle vérification MFA obligatoire, même avec une session ouverte.
    const mfaCode = prompt("Approbation d'un retrait : entrez votre code MFA (6 chiffres) ou un code de récupération.");
    if (!mfaCode?.trim()) return;
    return act(id, () => apiClient.post(`${basePath}/${id}/approve`, { mfaCode: mfaCode.trim() }));
  };
  const returnForCorrection = (id: string) => {
    const notes = askJustification("Motif du renvoi pour correction (donnée ou justificatif à rectifier) :");
    if (notes) return act(id, () => apiClient.patch(`${basePath}/${id}/return`, { notes }));
  };
  const reject = (id: string) => {
    const notes = askJustification("Justification du rejet (obligatoire) :");
    if (notes) return act(id, () => apiClient.patch(`${basePath}/${id}/reject`, { notes }));
  };
  const processing = (id: string) => act(id, () => apiClient.patch(`${basePath}/${id}/processing`, {}));
  const paid = (id: string) => {
    const notes = prompt("Référence du virement (optionnel) :") ?? "";
    return act(id, () => apiClient.patch(`${basePath}/${id}/paid`, { notes }));
  };
  const failed = (id: string) => {
    const notes = askJustification("Raison de l'échec du virement :");
    if (notes) return act(id, () => apiClient.patch(`${basePath}/${id}/failed`, { notes }));
  };

  async function showHistory(id: string) {
    if (historyFor === id) {
      setHistoryFor(null);
      return;
    }
    try {
      setHistory(await apiClient.get<WithdrawalHistoryEntry[]>(`${basePath}/${id}/history`));
      setHistoryFor(id);
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    }
  }

  function actionsFor(row: WithdrawalRow) {
    const busy = actingId === row.id;
    const buttons: React.ReactNode[] = [];
    if (canPrepare) {
      if (row.status === "PENDING" || row.status === "NEEDS_CORRECTION") {
        buttons.push(<Button key="prep" onClick={() => prepare(row.id)} disabled={busy}>Vérifié, préparer</Button>);
      }
      if (row.status === "PENDING" || row.status === "PREPARED") {
        buttons.push(
          <Button key="ret" variant="secondary" onClick={() => returnForCorrection(row.id)} disabled={busy}>
            Renvoyer
          </Button>,
        );
      }
      if (row.status === "PREPARED") {
        buttons.push(<Button key="sub" onClick={() => submit(row.id)} disabled={busy}>Soumettre au Super Admin</Button>);
      }
    }
    if (canApprove) {
      if (row.status === "SUBMITTED") {
        buttons.push(<Button key="app" onClick={() => approve(row.id)} disabled={busy}>Approuver (MFA)</Button>);
        buttons.push(<Button key="rej" variant="danger" onClick={() => reject(row.id)} disabled={busy}>Rejeter</Button>);
      }
      if (row.status === "APPROVED" || row.status === "FAILED") {
        buttons.push(<Button key="proc" variant="secondary" onClick={() => processing(row.id)} disabled={busy}>Lancer le virement</Button>);
      }
      if (row.status === "APPROVED" || row.status === "PROCESSING") {
        buttons.push(<Button key="paid" onClick={() => paid(row.id)} disabled={busy}>Marquer payé</Button>);
        buttons.push(<Button key="fail" variant="danger" onClick={() => failed(row.id)} disabled={busy}>Échec</Button>);
      }
    }
    return buttons;
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap gap-2">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            onClick={() => {
              setFilter(f.value);
              setPage(0);
            }}
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
            <EmptyState message={`Aucun ${title} dans cette catégorie.`} />
          ) : (
            <Table headers={[...columns, "Statut", "Actions"]}>
              {data.content.flatMap((row) => [
                <tr key={row.id}>
                  {renderCells(row)}
                  <td className="px-4 py-3">
                    <StatusBadge status={row.status} label={STATUS_LABELS[row.status]} />
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      {actionsFor(row)}
                      <Button variant="secondary" onClick={() => showHistory(row.id)}>
                        {historyFor === row.id ? "Masquer l'historique" : "Historique"}
                      </Button>
                    </div>
                  </td>
                </tr>,
                historyFor === row.id ? (
                  <tr key={`${row.id}-history`}>
                    <td colSpan={columns.length + 2} className="bg-input-fill px-4 py-3">
                      <ul className="space-y-1 text-xs text-muted">
                        {history.map((h) => (
                          <li key={h.id}>
                            <span className="font-semibold text-foreground">{formatDate(h.createdAt)}</span>{" "}
                            {h.fromStatus ? `${STATUS_LABELS[h.fromStatus]} → ` : ""}
                            {STATUS_LABELS[h.toStatus]}
                            {h.note ? ` — ${h.note}` : ""}
                          </li>
                        ))}
                        {row.approvedAt && (
                          <li>
                            Approuvé le {formatDate(row.approvedAt)} par le compte {row.approvedBy}
                          </li>
                        )}
                        {row.notes && <li className="whitespace-pre-line">Notes : {row.notes}</li>}
                      </ul>
                    </td>
                  </tr>
                ) : null,
              ])}
            </Table>
          )}
        </Card>
      )}
      {data && <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  );
}

export { formatFcfa };
