"use client";

import { useEffect, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { CourierEarningResponse, CourierEarningStatus, Page } from "@/lib/types";
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

export default function CourierEarningsPage() {
  const [status, setStatus] = useState<CourierEarningStatus | null>("PENDING");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<CourierEarningResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<CourierEarningResponse>>("/api/admin/courier-earnings", { status: status ?? undefined, page, size: 20 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [status, page]);

  async function markPaid(id: string) {
    setActingId(id);
    try {
      await apiClient.patch(`/api/admin/courier-earnings/${id}/paid`);
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setActingId(null);
    }
  }

  return (
    <div>
      <PageHeader title="Gains livreurs" subtitle="75% du tarif de zone par livraison terminée, 25% pour la plateforme." />

      <div className="mb-4 flex gap-2">
        {([["PENDING", "À payer"], ["PAID", "Payés"], [null, "Tous"]] as const).map(([value, label]) => (
          <button
            key={label}
            onClick={() => {
              setStatus(value);
              setPage(0);
            }}
            className={`rounded-full px-3 py-1.5 text-sm font-medium transition ${
              status === value ? "bg-primary text-white" : "bg-surface text-muted ring-1 ring-border"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <Card>
          {data.content.length === 0 ? (
            <EmptyState message="Aucun gain." />
          ) : (
            <Table headers={["Livreur", "Brut", "Commission (25%)", "Net livreur", "Statut", "Date", "Actions"]}>
              {data.content.map((e) => (
                <tr key={e.id}>
                  <td className="px-4 py-3 font-medium text-foreground">{e.agentName}</td>
                  <td className="px-4 py-3 text-muted">{formatFcfa(e.grossAmount)}</td>
                  <td className="px-4 py-3 text-muted">{formatFcfa(e.commissionAmount)}</td>
                  <td className="px-4 py-3 font-semibold text-foreground">{formatFcfa(e.amount)}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={e.status} />
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(e.createdAt)}</td>
                  <td className="px-4 py-3">
                    {e.status === "PENDING" && (
                      <Button onClick={() => markPaid(e.id)} disabled={actingId === e.id}>
                        Marquer payé
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </Table>
          )}
        </Card>
      )}
      {data && <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  );
}
