"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { Page, ShopResponse, ShopStatus } from "@/lib/types";
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

const STATUS_FILTERS: { value: ShopStatus | null; label: string }[] = [
  { value: "PENDING", label: "En attente" },
  { value: "APPROVED", label: "Approuvées" },
  { value: "SUSPENDED", label: "Suspendues" },
  { value: "REJECTED", label: "Rejetées" },
  { value: null, label: "Toutes" },
];

export default function ShopsPage() {
  const [status, setStatus] = useState<ShopStatus | null>("PENDING");
  const [data, setData] = useState<Page<ShopResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<ShopResponse>>("/api/admin/shops", { status: status ?? undefined, size: 50 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [status]);

  async function setStatusFor(id: string, newStatus: ShopStatus) {
    setActingId(id);
    try {
      await apiClient.patch(`/api/admin/shops/${id}/status`, { status: newStatus });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur lors de la décision.");
    } finally {
      setActingId(null);
    }
  }

  return (
    <div>
      <PageHeader title="Boutiques" subtitle="Approbation et modération des boutiques vendeurs" />

      <div className="mb-4 flex gap-2">
        {STATUS_FILTERS.map((f) => (
          <button
            key={f.label}
            onClick={() => setStatus(f.value)}
            className={`rounded-full px-3 py-1.5 text-sm font-medium transition ${
              status === f.value ? "bg-primary text-white" : "bg-surface text-muted ring-1 ring-border"
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
            <EmptyState message="Aucune boutique." />
          ) : (
            <Table headers={["Boutique", "Ville", "Statut", "Créée le", "Actions"]}>
              {data.content.map((s) => (
                <tr key={s.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{s.name}</td>
                  <td className="px-4 py-3 text-muted">
                    {s.city}
                    {s.district ? `, ${s.district}` : ""}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={s.status} />
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(s.createdAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      {s.status !== "APPROVED" && (
                        <Button onClick={() => setStatusFor(s.id, "APPROVED")} disabled={actingId === s.id}>
                          Approuver
                        </Button>
                      )}
                      {s.status === "APPROVED" && (
                        <Button
                          variant="secondary"
                          onClick={() => setStatusFor(s.id, "SUSPENDED")}
                          disabled={actingId === s.id}
                        >
                          Suspendre
                        </Button>
                      )}
                      {s.status !== "REJECTED" && (
                        <Button
                          variant="danger"
                          onClick={() => setStatusFor(s.id, "REJECTED")}
                          disabled={actingId === s.id}
                        >
                          Rejeter
                        </Button>
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
