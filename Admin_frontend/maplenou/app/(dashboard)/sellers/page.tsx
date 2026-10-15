"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { Page, SellerProfileResponse, SellerStatus } from "@/lib/types";
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

const STATUS_FILTERS: { value: SellerStatus; label: string }[] = [
  { value: "PENDING", label: "En attente" },
  { value: "APPROVED", label: "Approuvés" },
  { value: "REJECTED", label: "Rejetés" },
  { value: null, label: "Tous" },
];

export default function SellersPage() {
  const [status, setStatus] = useState<SellerStatus>("PENDING");
  const [data, setData] = useState<Page<SellerProfileResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<SellerProfileResponse>>("/api/sellers/admin", { status: status ?? undefined, size: 50 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [status]);

  async function decide(id: string, newStatus: "APPROVED" | "REJECTED") {
    setActingId(id);
    try {
      await apiClient.patch(`/api/sellers/admin/${id}/status`, { status: newStatus, adminNote: null });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur lors de la décision.");
    } finally {
      setActingId(null);
    }
  }

  return (
    <div>
      <PageHeader title="Candidatures vendeurs" subtitle="Approuver ou rejeter une demande d'ouverture de boutique" />

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
            <EmptyState message="Aucune candidature." />
          ) : (
            <Table headers={["Boutique", "Candidat", "Catégorie", "Statut", "Demandé le", "Actions"]}>
              {data.content.map((s) => (
                <tr key={s.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{s.shopName}</td>
                  <td className="px-4 py-3">
                    <div>{s.userFullName}</div>
                    <div className="text-xs text-muted">{s.userPhoneNumber}</div>
                  </td>
                  <td className="px-4 py-3 text-muted">{s.feeCategory === "HIGH_PRICE" ? "Prix élevés" : "Standard"}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={s.status} />
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(s.createdAt)}</td>
                  <td className="px-4 py-3">
                    {s.status === "PENDING" && (
                      <div className="flex gap-2">
                        <Button
                          onClick={() => decide(s.id, "APPROVED")}
                          disabled={actingId === s.id}
                        >
                          Approuver
                        </Button>
                        <Button
                          variant="danger"
                          onClick={() => decide(s.id, "REJECTED")}
                          disabled={actingId === s.id}
                        >
                          Rejeter
                        </Button>
                      </div>
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
