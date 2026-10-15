"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { Page, SellerFeeCategoryChangeResponse, SellerFeeCategoryChangeStatus } from "@/lib/types";
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
} from "@/components/ui";

const FILTERS: { label: string; value: SellerFeeCategoryChangeStatus | "ALL" }[] = [
  { label: "En attente", value: "PENDING" },
  { label: "Approuvées", value: "APPROVED" },
  { label: "Rejetées", value: "REJECTED" },
  { label: "Toutes", value: "ALL" },
];

export default function SellerFeesPage() {
  const [filter, setFilter] = useState<SellerFeeCategoryChangeStatus | "ALL">("PENDING");
  const [data, setData] = useState<Page<SellerFeeCategoryChangeResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<SellerFeeCategoryChangeResponse>>("/api/admin/seller-fees/category-changes", {
        status: filter === "ALL" ? undefined : filter,
        size: 50,
      })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [filter]);

  async function decide(id: string, approve: boolean) {
    const adminNotes = !approve ? prompt("Raison du rejet (optionnel) :") ?? "" : null;
    setActingId(id);
    try {
      await apiClient.patch(`/api/admin/seller-fees/category-changes/${id}/decide`, { approve, adminNotes });
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
        title="Changements de catégorie tarifaire"
        subtitle="Demandes de passage STANDARD ↔ HIGH_PRICE (barème de commission à 2 tranches)"
      />

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
            <EmptyState message="Aucune demande." />
          ) : (
            <Table headers={["Boutique", "Catégorie demandée", "Statut", "Effective le", "Demandée le", "Actions"]}>
              {data.content.map((r) => (
                <tr key={r.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{r.shopName}</td>
                  <td className="px-4 py-3 text-muted">{r.requestedCategory}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={r.status} />
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(r.effectiveFrom)}</td>
                  <td className="px-4 py-3 text-muted">{formatDate(r.createdAt)}</td>
                  <td className="px-4 py-3">
                    {r.status === "PENDING" ? (
                      <div className="flex gap-2">
                        <Button onClick={() => decide(r.id, true)} disabled={actingId === r.id}>
                          Approuver
                        </Button>
                        <Button variant="danger" onClick={() => decide(r.id, false)} disabled={actingId === r.id}>
                          Rejeter
                        </Button>
                      </div>
                    ) : (
                      <span className="text-sm text-muted">—</span>
                    )}
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
